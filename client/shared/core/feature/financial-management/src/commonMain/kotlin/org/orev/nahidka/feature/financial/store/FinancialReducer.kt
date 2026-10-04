package org.orev.nahidka.feature.financial.store

import org.orev.nahidka.core.common.ArithmeticOverflowException
import org.orev.nahidka.core.common.applyTo
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.financial.calculation.*
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*

internal class FinancialReducer(
    private val config: FinancialSessionConfig,
    private val calendar: FinancialCalendar,
) {

    private val assets = config.assets.associateBy {
        it.identifier
    }

    fun reduce(data: FinancialData, revision: Long, command: FinancialCommand): FinancialReduction = try {
        when (command) {
            is FinancialCommand.AddOperation -> addOperation(data, revision, command.command.operation)
            is FinancialCommand.UpdateOperation -> updateOperation(data, revision, command.command)
            is FinancialCommand.RemoveOperation -> removeOperation(data, revision, command.command)
            is FinancialCommand.SavePlanning -> savePlanning(data, revision, command.command)
            is FinancialCommand.DeletePlanning -> deletePlanning(data, revision, command.command)
            is FinancialCommand.CreateCategory -> createCategory(data, revision, command.command)
            is FinancialCommand.UpdateCategory -> updateCategory(data, command.command)
            is FinancialCommand.ArchiveCategory -> archiveCategory(data, command.command)
            is FinancialCommand.DeleteCategory -> deleteCategory(data, command.command)
        }
    } catch (_: ArithmeticOverflowException) {
        FinancialReduction.Rejected(FinancialError.Validation("amount", "The amount exceeds the supported range"))
    }

    private fun addOperation(data: FinancialData, revision: Long, input: NewFinancialOperation): FinancialReduction {
        if (!validIdentifier(input.identifier)) {
            return reject("identifier", "A valid operation ID is required")
        }

        if (input.identifier in data.operations) {
            return reject(
                "identifier",
                "An operation with this identifier already exists"
            )
        }

        if (data.operations.size >= config.maxOperations) {
            return reject(
                "operations",
                "The session operation limit has been reached"
            )
        }

        val categoryIdentifier = if (input.kind == OperationKind.REFUND) {
            val parentIdentifier = input.refundOfOperationIdentifier ?: return reject(
                "refundOfOperationIdentifier",
                "A refund must reference an expense"
            )

            val parent = data.operations[parentIdentifier]
                ?: return FinancialReduction.Rejected(FinancialError.NotFound("operation", parentIdentifier))

            if (parent.kind != OperationKind.EXPENSE) {
                return reject(
                    "refundOfOperationIdentifier",
                    "A refund must reference an expense"
                )
            }

            if (input.amount.assetIdentifier != parent.amount.assetIdentifier) {
                return reject(
                    "amount",
                    "A refund must use the expense asset"
                )
            }

            if (input.categoryIdentifier != null && input.categoryIdentifier != parent.categoryIdentifier) {
                return reject(
                    "categoryIdentifier",
                    "A refund inherits its expense category"
                )
            }

            parent.categoryIdentifier
        } else {
            if (input.refundOfOperationIdentifier != null) return reject(
                "refundOfOperationIdentifier",
                "Only refunds may reference an expense"
            )

            input.categoryIdentifier
        }

        val operation = FinancialOperation(
            identifier = input.identifier,
            version = incrementRevision(revision),
            amount = input.amount,
            kind = input.kind,
            categoryIdentifier = categoryIdentifier,
            paymentMethod = input.paymentMethod,
            occurredAt = input.occurredAt,
            description = input.description,
            refundOfOperationIdentifier = input.refundOfOperationIdentifier,
        )

        validateOperation(data, operation, current = null)?.let {
            return FinancialReduction.Rejected(it)
        }

        val nextData = data.copy(
            operations = data.operations.putting(operation.identifier, operation),
        )

        validateAffectedTotals(nextData, listOf(operation))?.let { return FinancialReduction.Rejected(it) }
        return accepted(nextData, FinancialValue.Operation(operation))
    }

    private fun updateOperation(
        data: FinancialData,
        revision: Long,
        command: UpdateFinancialOperation
    ): FinancialReduction {
        if (!validIdentifier(command.identifier)) {
            return reject("identifier", "A valid operation ID is required")
        }

        val current = data.operations[command.identifier] ?: return FinancialReduction.Rejected(
            FinancialError.NotFound(
                "operation",
                command.identifier
            )
        )

        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.OperationConflict(command.expectedVersion, current))
        }

        val next = current.apply(command.patch)
        validateOperation(data, next, current)?.let {
            return FinancialReduction.Rejected(it)
        }

        if (next.copy(version = current.version) == current) {
            return FinancialReduction.Accepted(data, FinancialValue.Operation(current), false)
        }

        val versioned = next.copy(version = incrementRevision(current.version))
        val nextData = data.copy(operations = data.operations.putting(current.identifier, versioned))

        validateAffectedTotals(nextData, listOf(current, versioned))?.let { return FinancialReduction.Rejected(it) }
        return accepted(
            nextData,
            FinancialValue.Operation(versioned),
        )
    }

    private fun removeOperation(
        data: FinancialData,
        revision: Long,
        command: RemoveFinancialOperation
    ): FinancialReduction {
        if (!validIdentifier(command.identifier)) {
            return reject("identifier", "A valid operation ID is required")
        }

        val current = data.operations[command.identifier] ?: return FinancialReduction.Rejected(
            FinancialError.NotFound(
                "operation",
                command.identifier
            )
        )
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.OperationConflict(command.expectedVersion, current))
        }

        if (current.kind == OperationKind.EXPENSE && data.operations.values.any { it.refundOfOperationIdentifier == current.identifier }) {
            return reject("identifier", "An expense with linked refunds cannot be deleted")
        }

        val nextData = data.copy(operations = data.operations.removing(current.identifier))

        validateAffectedTotals(nextData, listOf(current))?.let {
            return FinancialReduction.Rejected(it)
        }
        return accepted(nextData, FinancialValue.Operation(current))
    }

    private fun validateAffectedTotals(
        data: FinancialData,
        operations: List<FinancialOperation>,
    ): FinancialError? {
        val periods = operations.map { operation ->
            operation.amount.assetIdentifier to financialMonthFor(operation, config.reportingTimeZone)
        }.toSet()

        for ((assetIdentifier, month) in periods) {
            val period = calendar.reportingPeriod(month, config.reportingTimeZone)
            val table =
                data.planningTables.values.firstOrNull { it.assetIdentifier == assetIdentifier && it.month == month }
            validateFinancialTotals(data.operations.values, table, period, assetIdentifier)
        }

        return null
    }

    private fun validateOperation(
        data: FinancialData,
        operation: FinancialOperation,
        current: FinancialOperation?,
    ): FinancialError? {
        if (operation.amount.units <= 0) {
            return FinancialError.Validation(
                "amount",
                "An operation amount must be positive"
            )
        }

        if (operation.amount.assetIdentifier !in assets) {
            return FinancialError.Validation(
                "amount.assetIdentifier",
                "The asset is not configured for this session"
            )
        }

        if (operation.description != null && operation.description.length > config.maxDescriptionLength) {
            return FinancialError.Validation("description", "The description is too long")
        }

        if (operation.kind == OperationKind.REFUND) {
            val parentIdentifier = operation.refundOfOperationIdentifier
                ?: return FinancialError.Validation("refundOfOperationIdentifier", "A refund must reference an expense")
            val parent = data.operations[parentIdentifier]
                ?: return FinancialError.NotFound("operation", parentIdentifier)

            if (parent.kind != OperationKind.EXPENSE) {
                return FinancialError.Validation(
                    "refundOfOperationIdentifier",
                    "A refund must reference an expense"
                )
            }

            if (parent.amount.assetIdentifier != operation.amount.assetIdentifier) {
                return FinancialError.Validation(
                    "amount.assetIdentifier",
                    "A refund must use the expense asset"
                )
            }

            if (parent.categoryIdentifier != operation.categoryIdentifier) {
                return FinancialError.Validation(
                    "categoryIdentifier",
                    "A refund inherits its expense category"
                )
            }

            val existingRefunds = data.operations.values
                .asSequence()
                .filter { it.identifier != operation.identifier && it.refundOfOperationIdentifier == parent.identifier }
                .fold(0L) { total, refund -> checkedAdd(total, refund.amount.units) }

            if (checkedAdd(existingRefunds, operation.amount.units) > parent.amount.units) {
                return FinancialError.Validation("amount", "Refunds cannot exceed the linked expense")
            }
        } else if (operation.refundOfOperationIdentifier != null) {
            return FinancialError.Validation("refundOfOperationIdentifier", "Only refunds may reference an expense")
        }

        if (operation.kind == OperationKind.EXPENSE && operation.categoryIdentifier == null) {
            return FinancialError.Validation("categoryIdentifier", "An expense must have a category")
        }

        operation.categoryIdentifier?.let { categoryIdentifier ->
            val category = data.categories[categoryIdentifier]
                ?: return FinancialError.NotFound("category", categoryIdentifier)
            val unchangedArchivedReference = current?.categoryIdentifier == categoryIdentifier && category.archived

            if (category.archived && !unchangedArchivedReference && operation.kind != OperationKind.REFUND) {
                return FinancialError.Validation(
                    "categoryIdentifier",
                    "Archived categories cannot be used for new operations"
                )
            }
        }

        if (current?.kind == OperationKind.EXPENSE) {
            val refunds = data.operations.values
                .asSequence()
                .filter { it.refundOfOperationIdentifier == current.identifier }
                .fold(0L) { total, refund -> checkedAdd(total, refund.amount.units) }
            val hasRefunds = refunds > 0

            if (hasRefunds && operation.kind != OperationKind.EXPENSE) {
                return FinancialError.Validation("kind", "An expense with linked refunds cannot change kind")
            }

            if (hasRefunds && operation.amount.assetIdentifier != current.amount.assetIdentifier) {
                return FinancialError.Validation(
                    "amount.assetIdentifier",
                    "The asset cannot change while refunds are linked"
                )
            }

            if (hasRefunds && operation.categoryIdentifier != current.categoryIdentifier) {
                return FinancialError.Validation(
                    "categoryIdentifier",
                    "The category cannot change while refunds are linked"
                )
            }

            if (operation.kind == OperationKind.EXPENSE && refunds > operation.amount.units) {
                return FinancialError.Validation("amount", "The expense cannot be lower than its linked refunds")
            }
        }

        return null
    }

    private fun createCategory(
        data: FinancialData,
        revision: Long,
        command: CreateFinancialCategory
    ): FinancialReduction {
        if (!validIdentifier(command.identifier)) {
            return reject("identifier", "A valid category ID is required")
        }

        if (command.identifier in data.categories) {
            return reject(
                "identifier",
                "A category with this identifier already exists"
            )
        }

        if (data.categories.size >= config.maxCategories) {
            return reject(
                "categories",
                "The session category limit has been reached"
            )
        }

        val name = validatedCategoryName(command.name) ?: return reject(
            "name",
            "Category names must contain 1 to ${config.maxCategoryNameLength} characters"
        )

        if (data.categories.values.any { categoryNameKey(it.name) == categoryNameKey(name) }) {
            return reject("name", "A category with this name already exists")
        }

        if (command.iconName != null && command.iconName.length > 64) {
            return reject(
                "iconName",
                "The icon name is too long"
            )
        }

        val category = FinancialCategory(command.identifier, incrementRevision(revision), name, command.iconName, false)

        return accepted(
            data.copy(
                categories = data.categories.putting(category.identifier, category),
            ),
            FinancialValue.Category(category),
        )
    }

    private fun updateCategory(data: FinancialData, command: UpdateFinancialCategory): FinancialReduction {
        val current = data.categories[command.identifier] ?: return FinancialReduction.Rejected(
            FinancialError.NotFound(
                "category",
                command.identifier
            )
        )

        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }

        val name = command.name?.let {
            validatedCategoryName(it) ?: return reject(
                "name",
                "Category names must contain 1 to ${config.maxCategoryNameLength} characters"
            )
        } ?: current.name

        if (data.categories.values.any {
                it.identifier != current.identifier && categoryNameKey(it.name) == categoryNameKey(
                    name
                )
            }) {
            return reject("name", "A category with this name already exists")
        }
        val iconName = command.iconName.applyTo(current.iconName)

        if (iconName != null && iconName.length > 64) {
            return reject("iconName", "The icon name is too long")
        }

        val updated = current.copy(name = name, iconName = iconName)

        if (updated == current) {
            return FinancialReduction.Accepted(data, FinancialValue.Category(current), false)
        }

        val versioned = updated.copy(version = incrementRevision(current.version))

        return accepted(
            data.copy(categories = data.categories.putting(current.identifier, versioned)),
            FinancialValue.Category(versioned)
        )
    }

    private fun archiveCategory(data: FinancialData, command: ArchiveFinancialCategory): FinancialReduction {
        val current = data.categories[command.identifier] ?: return FinancialReduction.Rejected(
            FinancialError.NotFound(
                "category",
                command.identifier
            )
        )

        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }

        if (current.archived) {
            return FinancialReduction.Accepted(data, FinancialValue.Category(current), false)
        }

        val archived = current.copy(version = incrementRevision(current.version), archived = true)

        return accepted(
            data.copy(categories = data.categories.putting(current.identifier, archived)),
            FinancialValue.Category(archived)
        )
    }

    private fun deleteCategory(data: FinancialData, command: DeleteFinancialCategory): FinancialReduction {
        val current = data.categories[command.identifier] ?: return FinancialReduction.Rejected(
            FinancialError.NotFound(
                "category",
                command.identifier
            )
        )

        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }

        val usedByOperations = data.operations.values.any {
            it.categoryIdentifier == current.identifier
        }

        val usedByPlans = data.planningTables.values.any { table ->
            table.rows.any { it.categoryIdentifier == current.identifier }
        }

        if (usedByOperations || usedByPlans) {
            return FinancialReduction.Rejected(FinancialError.CategoryInUse(current.identifier))
        }

        return accepted(
            data.copy(categories = data.categories.removing(current.identifier)),
            FinancialValue.Category(current)
        )
    }

    private fun savePlanning(data: FinancialData, revision: Long, command: SavePlanningTable): FinancialReduction {
        val input = command.table

        if (!validIdentifier(input.identifier)) {
            return reject(
                "table.identifier",
                "A valid planning table ID is required"
            )
        }

        val current = data.planningTables[input.identifier]
        val samePeriod = data.planningTables.values.firstOrNull {
            it.month == input.month && it.assetIdentifier == input.assetIdentifier
        }

        if (command.expectedVersion == null) {
            if (current != null) {
                return FinancialReduction.Rejected(FinancialError.PlanningConflict(null, current))
            }

            if (samePeriod != null) {
                return FinancialReduction.Rejected(
                    FinancialError.PlanningConflict(
                        null,
                        samePeriod
                    )
                )
            }

            if (data.planningTables.size >= config.maxPlanningTables) {
                return reject(
                    "table",
                    "The session planning table limit has been reached"
                )
            }
        } else {
            if (current == null) {
                if (samePeriod != null) {
                    return FinancialReduction.Rejected(
                        FinancialError.PlanningConflict(
                            command.expectedVersion,
                            samePeriod
                        )
                    )
                }

                return FinancialReduction.Rejected(FinancialError.NotFound("planning table", input.identifier))
            }

            if (current.version != command.expectedVersion) {
                return FinancialReduction.Rejected(FinancialError.PlanningConflict(command.expectedVersion, current))
            }

            if (current.month != input.month || current.assetIdentifier != input.assetIdentifier) {
                return reject("table", "A planning table's month and asset cannot change")
            }
        }

        validatePlanningInput(data, input, current)?.let {
            return FinancialReduction.Rejected(it)
        }

        if (current != null && current == input.toPlanningTable(current.version)) {
            val period = calendar.reportingPeriod(input.month, config.reportingTimeZone)
            val view = calculatePlanningTableView(current, data.operations.values, data.categories.values, period)

            return FinancialReduction.Accepted(data, FinancialValue.PlanningTable(current, view), false)
        }

        val version = if (current == null) {
            incrementRevision(revision)
        } else {
            incrementRevision(current.version)
        }

        val next = input.toPlanningTable(version)

        val nextData = data.copy(
            planningTables = data.planningTables.putting(input.identifier, next),
        )

        val period = calendar.reportingPeriod(input.month, config.reportingTimeZone)
        val projected = calculatePlanningTableView(next, nextData.operations.values, nextData.categories.values, period)

        return accepted(nextData, FinancialValue.PlanningTable(next, projected))
    }

    private fun validatePlanningInput(
        data: FinancialData,
        input: FinancialPlanningTableInput,
        current: FinancialPlanningTable?,
    ): FinancialError? {
        if (input.assetIdentifier !in assets) {
            return FinancialError.Validation(
                "table.assetIdentifier",
                "The asset is not configured for this session"
            )
        }

        if (input.openingAvailable.assetIdentifier != input.assetIdentifier) {
            return FinancialError.Validation(
                "openingAvailable",
                "Opening funds must use the table asset"
            )
        }
        if (input.savingsPolicy != null) {
            if (input.savingsPolicy.reserve.assetIdentifier != input.assetIdentifier) {
                return FinancialError.Validation(
                    "savingsPolicy.reserve",
                    "The reserve must use the table asset"
                )
            }
            if (input.savingsPolicy.reserve.units < 0) {
                return FinancialError.Validation(
                    "savingsPolicy.reserve",
                    "The reserve cannot be negative"
                )
            }
            if (input.savingsPolicy.allocationBasisPoints !in 0..10_000) {
                return FinancialError.Validation(
                    "savingsPolicy.allocationBasisPoints",
                    "The allocation rate must be between 0 and 10,000 basis points"
                )
            }
        }
        if (input.rows.size > config.maxPlanningRows) {
            return FinancialError.Validation(
                "rows",
                "The planning table row limit has been reached"
            )
        }

        val rowIdentifiers = HashSet<String>(input.rows.size)
        val categoryIdentifiers = HashSet<String>(input.rows.size)

        for (row in input.rows) {
            if (!rowIdentifiers.add(row.identifier)) {
                return FinancialError.Validation(
                    "rows",
                    "Planning row IDs must be unique"
                )
            }

            if (!categoryIdentifiers.add(row.categoryIdentifier)) {
                return FinancialError.Validation(
                    "rows",
                    "A category can appear only once in a monthly table"
                )
            }
        }

        val existingRows = current?.rows?.associateBy { it.identifier }.orEmpty()
        val existingIdentifiers = existingRows.keys
        val activeRowIdentifiers = HashSet<String>()

        for (table in data.planningTables.values) {
            if (table.identifier == input.identifier) {
                continue
            }

            for (row in table.rows) {
                activeRowIdentifiers.add(row.identifier)
            }
        }

        for (row in input.rows) {
            if (!validIdentifier(row.identifier)) {
                return FinancialError.Validation(
                    "rows.identifier",
                    "A valid planning row ID is required"
                )
            }

            if (row.identifier in activeRowIdentifiers) {
                return FinancialError.Validation(
                    "rows.identifier",
                    "A planning row identifier must be unique across active tables"
                )
            }

            if (row.categoryIdentifier.isBlank()) {
                return FinancialError.Validation(
                    "rows.categoryIdentifier",
                    "A category is required"
                )
            }

            if (row.plannedAmount.assetIdentifier != input.assetIdentifier) {
                return FinancialError.Validation(
                    "rows.plannedAmount",
                    "Planned amounts must use the table asset"
                )
            }

            if (row.plannedAmount.units < 0) {
                return FinancialError.Validation(
                    "rows.plannedAmount",
                    "A planned amount cannot be negative"
                )
            }

            val category = data.categories[row.categoryIdentifier]
                ?: return FinancialError.NotFound("category", row.categoryIdentifier)
            val retainedArchived = row.identifier in existingIdentifiers
                    && existingRows[row.identifier]?.categoryIdentifier == row.categoryIdentifier

            if (category.archived && !retainedArchived) {
                return FinancialError.Validation(
                    "rows.categoryIdentifier",
                    "Archived categories cannot be added to a plan"
                )
            }
        }

        return null
    }

    private fun deletePlanning(data: FinancialData, revision: Long, command: DeletePlanningTable): FinancialReduction {
        val current = data.planningTables[command.identifier]
            ?: return FinancialReduction.Rejected(FinancialError.NotFound("planning table", command.identifier))

        if (current.version != command.expectedVersion) {
            return FinancialReduction.Rejected(FinancialError.PlanningConflict(command.expectedVersion, current))
        }

        val period = calendar.reportingPeriod(current.month, config.reportingTimeZone)
        val view = calculatePlanningTableView(current, data.operations.values, data.categories.values, period)

        return accepted(
            data.copy(planningTables = data.planningTables.removing(current.identifier)),
            FinancialValue.PlanningTable(current, view)
        )
    }

    private fun validIdentifier(identifier: String): Boolean = identifier.isNotBlank() && identifier.length <= 128

    private fun validatedCategoryName(value: String): String? {
        val name = value.trim()
        return name.takeIf { it.isNotEmpty() && it.length <= config.maxCategoryNameLength }
    }

    private fun categoryNameKey(value: String): String = value.trim().lowercase()

    private fun accepted(data: FinancialData, value: FinancialValue): FinancialReduction =
        FinancialReduction.Accepted(data, value, true)

    private fun reject(field: String, message: String): FinancialReduction =
        FinancialReduction.Rejected(FinancialError.Validation(field, message))
}
