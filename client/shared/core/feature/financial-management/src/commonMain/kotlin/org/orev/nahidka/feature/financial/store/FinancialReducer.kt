package org.orev.nahidka.feature.financial.store

import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.apply
import org.orev.nahidka.feature.financial.calculation.calculatePlanningTableView
import org.orev.nahidka.feature.financial.calculation.calculateSpendingSummary
import org.orev.nahidka.feature.financial.calculation.checkedAdd
import org.orev.nahidka.feature.financial.calculation.checkedNextVersion
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.DeletePlanningTable
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.command.applyTo
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.support.FinancialOverflowException

internal class FinancialReducer(
    private val config: FinancialSessionConfig,
    private val calendar: FinancialCalendar,
) {
    private val assets = config.assets.associateBy { it.id }

    fun reduce(data: FinancialData, revision: Long, command: FinancialCommand): FinancialReduction = try {
        when (command) {
            is FinancialCommand.AddOperation -> addOperation(data, revision, command.command.operation)
            is FinancialCommand.UpdateOperation -> updateOperation(data, revision, command.command)
            is FinancialCommand.RemoveOperation -> removeOperation(data, revision, command.command)
            is FinancialCommand.SavePlanning -> savePlanning(data, revision, command.command)
            is FinancialCommand.DeletePlanning -> deletePlanning(data, revision, command.command)
            is FinancialCommand.CreateCategory -> createCategory(data, command.command)
            is FinancialCommand.UpdateCategory -> updateCategory(data, command.command)
            is FinancialCommand.ArchiveCategory -> archiveCategory(data, command.command)
            is FinancialCommand.DeleteCategory -> deleteCategory(data, command.command)
        }
    } catch (_: FinancialOverflowException) {
        FinancialReduction.Rejected(FinancialError.Validation("amount", "The amount exceeds the supported range"))
    }

    private fun addOperation(data: FinancialData, revision: Long, input: NewFinancialOperation): FinancialReduction {
        if (!validId(input.id)) return reject("id", "A valid operation ID is required")
        if (input.id in data.usedOperationIds) return reject("id", "Operation IDs cannot be reused")
        if (data.operations.size >= config.maxOperations) return reject("operations", "The session operation limit has been reached")
        val categoryId = if (input.kind == OperationKind.REFUND) {
            val parentId = input.refundOfOperationId ?: return reject("refundOfOperationId", "A refund must reference an expense")
            val parent = data.operations[parentId] ?: return FinancialReduction.Rejected(FinancialError.NotFound("operation", parentId))
            if (parent.kind != OperationKind.EXPENSE) return reject("refundOfOperationId", "A refund must reference an expense")
            if (input.amount.assetId != parent.amount.assetId) return reject("amount", "A refund must use the expense asset")
            if (input.categoryId != null && input.categoryId != parent.categoryId) return reject("categoryId", "A refund inherits its expense category")
            parent.categoryId
        } else {
            if (input.refundOfOperationId != null) return reject("refundOfOperationId", "Only refunds may reference an expense")
            input.categoryId
        }
        val operation = FinancialOperation(
            id = input.id,
            version = 1,
            amount = input.amount,
            kind = input.kind,
            categoryId = categoryId,
            paymentMethod = input.paymentMethod,
            occurredAt = input.occurredAt,
            description = input.description,
            refundOfOperationId = input.refundOfOperationId,
        )
        validateOperation(data, operation, current = null)?.let { return FinancialReduction.Rejected(it) }
        val nextData = data.copy(
                operations = data.operations.putting(operation.id, operation),
                usedOperationIds = data.usedOperationIds.adding(operation.id),
            )
        validateAffectedProjections(nextData, revision, listOf(operation))?.let { return FinancialReduction.Rejected(it) }
        return accepted(nextData, FinancialValue.Operation(operation))
    }

    private fun updateOperation(data: FinancialData, revision: Long, command: UpdateFinancialOperation): FinancialReduction {
        if (!validId(command.id)) return reject("id", "A valid operation ID is required")
        val current = data.operations[command.id] ?: return FinancialReduction.Rejected(FinancialError.NotFound("operation", command.id))
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.OperationConflict(command.expectedVersion, current))
        }
        val next = current.apply(command.patch)
        validateOperation(data, next, current)?.let { return FinancialReduction.Rejected(it) }
        if (next.copy(version = current.version) == current) {
            return FinancialReduction.Accepted(data, FinancialValue.Operation(current), false)
        }
        val versioned = next.copy(version = checkedNextVersion(current.version))
        val nextData = data.copy(operations = data.operations.putting(current.id, versioned))
        validateAffectedProjections(nextData, revision, listOf(current, versioned))?.let { return FinancialReduction.Rejected(it) }
        return accepted(
            nextData,
            FinancialValue.Operation(versioned),
        )
    }

    private fun removeOperation(data: FinancialData, revision: Long, command: RemoveFinancialOperation): FinancialReduction {
        if (!validId(command.id)) return reject("id", "A valid operation ID is required")
        val current = data.operations[command.id] ?: return FinancialReduction.Rejected(FinancialError.NotFound("operation", command.id))
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.OperationConflict(command.expectedVersion, current))
        }
        if (current.kind == OperationKind.EXPENSE && data.operations.values.any { it.refundOfOperationId == current.id }) {
            return reject("id", "An expense with linked refunds cannot be deleted")
        }
        val nextData = data.copy(operations = data.operations.removing(current.id))
        validateAffectedProjections(nextData, revision, listOf(current))?.let { return FinancialReduction.Rejected(it) }
        return accepted(nextData, FinancialValue.Operation(current))
    }

    private fun validateAffectedProjections(
        data: FinancialData,
        revision: Long,
        operations: List<FinancialOperation>,
    ): FinancialError? {
        val periods = operations.map { operation ->
            operation.amount.assetId to financialMonthFor(operation, config.reportingTimeZone)
        }.toSet()
        for ((assetId, month) in periods) {
            val period = calendar.reportingPeriod(month, config.reportingTimeZone)
            val monthOperations = data.operations.values.filter {
                it.amount.assetId == assetId && it.occurredAt >= period.startInclusive && it.occurredAt < period.endExclusive
            }
            calculateSpendingSummary(monthOperations, data.categories.values, period, assetId, revision)
            val table = data.planningTables.values.firstOrNull { it.input.assetId == assetId && it.input.month == month }
            if (table != null) calculatePlanningTableView(table, monthOperations, data.categories.values, period, revision)
        }
        return null
    }

    private fun validateOperation(
        data: FinancialData,
        operation: FinancialOperation,
        current: FinancialOperation?,
    ): FinancialError? {
        if (operation.amount.units <= 0) return FinancialError.Validation("amount", "An operation amount must be positive")
        if (operation.amount.assetId !in assets) return FinancialError.Validation("amount.assetId", "The asset is not configured for this session")
        if (operation.description != null && operation.description.length > config.maxDescriptionLength) {
            return FinancialError.Validation("description", "The description is too long")
        }
        if (operation.kind == OperationKind.REFUND) {
            val parentId = operation.refundOfOperationId
                ?: return FinancialError.Validation("refundOfOperationId", "A refund must reference an expense")
            val parent = data.operations[parentId]
                ?: return FinancialError.NotFound("operation", parentId)
            if (parent.kind != OperationKind.EXPENSE) return FinancialError.Validation("refundOfOperationId", "A refund must reference an expense")
            if (parent.amount.assetId != operation.amount.assetId) return FinancialError.Validation("amount.assetId", "A refund must use the expense asset")
            if (parent.categoryId != operation.categoryId) return FinancialError.Validation("categoryId", "A refund inherits its expense category")
            val existingRefunds = data.operations.values
                .asSequence()
                .filter { it.id != operation.id && it.refundOfOperationId == parent.id }
                .fold(0L) { total, refund -> checkedAdd(total, refund.amount.units) }
            if (checkedAdd(existingRefunds, operation.amount.units) > parent.amount.units) {
                return FinancialError.Validation("amount", "Refunds cannot exceed the linked expense")
            }
        } else if (operation.refundOfOperationId != null) {
            return FinancialError.Validation("refundOfOperationId", "Only refunds may reference an expense")
        }
        if (operation.kind == OperationKind.EXPENSE && operation.categoryId == null) {
            return FinancialError.Validation("categoryId", "An expense must have a category")
        }
        operation.categoryId?.let { categoryId ->
            val category = data.categories[categoryId]
                ?: return FinancialError.NotFound("category", categoryId)
            val unchangedArchivedReference = current?.categoryId == categoryId && category.archived
            if (category.archived && !unchangedArchivedReference && operation.kind != OperationKind.REFUND) {
                return FinancialError.Validation("categoryId", "Archived categories cannot be used for new operations")
            }
        }
        if (current?.kind == OperationKind.EXPENSE) {
            val refunds = data.operations.values
                .asSequence()
                .filter { it.refundOfOperationId == current.id }
                .fold(0L) { total, refund -> checkedAdd(total, refund.amount.units) }
            val hasRefunds = refunds > 0
            if (hasRefunds && operation.kind != OperationKind.EXPENSE) {
                return FinancialError.Validation("kind", "An expense with linked refunds cannot change kind")
            }
            if (hasRefunds && operation.amount.assetId != current.amount.assetId) {
                return FinancialError.Validation("amount.assetId", "The asset cannot change while refunds are linked")
            }
            if (hasRefunds && operation.categoryId != current.categoryId) {
                return FinancialError.Validation("categoryId", "The category cannot change while refunds are linked")
            }
            if (operation.kind == OperationKind.EXPENSE && refunds > operation.amount.units) {
                return FinancialError.Validation("amount", "The expense cannot be lower than its linked refunds")
            }
        }
        return null
    }

    private fun createCategory(data: FinancialData, command: CreateFinancialCategory): FinancialReduction {
        if (!validId(command.id)) return reject("id", "A valid category ID is required")
        if (command.id in data.usedCategoryIds) return reject("id", "Category IDs cannot be reused")
        if (data.categories.size >= config.maxCategories) return reject("categories", "The session category limit has been reached")
        val name = validatedCategoryName(command.name) ?: return reject("name", "Category names must contain 1 to ${config.maxCategoryNameLength} characters")
        if (data.categories.values.any { categoryNameKey(it.name) == categoryNameKey(name) }) {
            return reject("name", "A category with this name already exists")
        }
        if (command.iconName != null && command.iconName.length > 64) return reject("iconName", "The icon name is too long")
        val category = FinancialCategory(command.id, 1, name, command.iconName, false)
        return accepted(
            data.copy(
                categories = data.categories.putting(category.id, category),
                usedCategoryIds = data.usedCategoryIds.adding(category.id),
            ),
            FinancialValue.Category(category),
        )
    }

    private fun updateCategory(data: FinancialData, command: UpdateFinancialCategory): FinancialReduction {
        val current = data.categories[command.id] ?: return FinancialReduction.Rejected(FinancialError.NotFound("category", command.id))
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }
        val name = command.name?.let { validatedCategoryName(it) ?: return reject("name", "Category names must contain 1 to ${config.maxCategoryNameLength} characters") }
            ?: current.name
        if (data.categories.values.any { it.id != current.id && categoryNameKey(it.name) == categoryNameKey(name) }) {
            return reject("name", "A category with this name already exists")
        }
        val iconName = command.iconName.applyTo(current.iconName)
        if (iconName != null && iconName.length > 64) return reject("iconName", "The icon name is too long")
        val updated = current.copy(name = name, iconName = iconName)
        if (updated == current) return FinancialReduction.Accepted(data, FinancialValue.Category(current), false)
        val versioned = updated.copy(version = checkedNextVersion(current.version))
        return accepted(data.copy(categories = data.categories.putting(current.id, versioned)), FinancialValue.Category(versioned))
    }

    private fun archiveCategory(data: FinancialData, command: ArchiveFinancialCategory): FinancialReduction {
        val current = data.categories[command.id] ?: return FinancialReduction.Rejected(FinancialError.NotFound("category", command.id))
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }
        if (current.archived) return FinancialReduction.Accepted(data, FinancialValue.Category(current), false)
        val archived = current.copy(version = checkedNextVersion(current.version), archived = true)
        return accepted(data.copy(categories = data.categories.putting(current.id, archived)), FinancialValue.Category(archived))
    }

    private fun deleteCategory(data: FinancialData, command: DeleteFinancialCategory): FinancialReduction {
        val current = data.categories[command.id] ?: return FinancialReduction.Rejected(FinancialError.NotFound("category", command.id))
        if (command.expectedVersion != current.version) {
            return FinancialReduction.Rejected(FinancialError.CategoryConflict(command.expectedVersion, current))
        }
        val usedByOperations = data.operations.values.any { it.categoryId == current.id }
        val usedByPlans = data.planningTables.values.any { table -> table.input.rows.any { it.categoryId == current.id } }
        if (usedByOperations || usedByPlans) return FinancialReduction.Rejected(FinancialError.CategoryInUse(current.id))
        return accepted(data.copy(categories = data.categories.removing(current.id)), FinancialValue.Category(current))
    }

    private fun savePlanning(data: FinancialData, revision: Long, command: SavePlanningTable): FinancialReduction {
        val input = command.table
        if (!validId(input.id)) return reject("table.id", "A valid planning table ID is required")
        val current = data.planningTables[input.id]
        val samePeriod = data.planningTables.values.firstOrNull {
            it.input.month == input.month && it.input.assetId == input.assetId
        }
        if (command.expectedVersion == null) {
            if (current != null) return FinancialReduction.Rejected(FinancialError.PlanningConflict(null, current))
            if (samePeriod != null) return FinancialReduction.Rejected(FinancialError.PlanningConflict(null, samePeriod))
            if (input.id in data.usedPlanningTableIds) return reject("table.id", "Planning table IDs cannot be reused")
            if (data.planningTables.size >= config.maxPlanningTables) return reject("table", "The session planning table limit has been reached")
        } else {
            if (current == null) {
                if (samePeriod != null) return FinancialReduction.Rejected(FinancialError.PlanningConflict(command.expectedVersion, samePeriod))
                return FinancialReduction.Rejected(FinancialError.NotFound("planning table", input.id))
            }
            if (current.version != command.expectedVersion) {
                return FinancialReduction.Rejected(FinancialError.PlanningConflict(command.expectedVersion, current))
            }
            if (current.input.month != input.month || current.input.assetId != input.assetId) {
                return reject("table", "A planning table's month and asset cannot change")
            }
        }
        validatePlanningInput(data, input, current)?.let { return FinancialReduction.Rejected(it) }
        if (current != null && current.input == input) {
            val period = calendar.reportingPeriod(input.month, config.reportingTimeZone)
            val view = calculatePlanningTableView(current, data.operations.values, data.categories.values, period, revision)
            return FinancialReduction.Accepted(data, FinancialValue.PlanningTable(current, view), false)
        }
        val version = if (current == null) 1L else checkedNextVersion(current.version)
        val next = FinancialPlanningTable(version, input)
        val oldRowIds = current?.input?.rows?.map { it.id }?.toSet().orEmpty()
        val newRowIds = input.rows.map { it.id }.toSet()
        val removedRowIds = oldRowIds - newRowIds
        val nextData = data.copy(
            planningTables = data.planningTables.putting(input.id, next),
            usedPlanningTableIds = data.usedPlanningTableIds.adding(input.id),
            usedPlanningRowIds = data.usedPlanningRowIds.addingAll(removedRowIds).addingAll(newRowIds),
        )
        val period = calendar.reportingPeriod(input.month, config.reportingTimeZone)
        val projected = calculatePlanningTableView(next, nextData.operations.values, nextData.categories.values, period, checkedNextVersion(revision))
        return accepted(nextData, FinancialValue.PlanningTable(next, projected))
    }

    private fun validatePlanningInput(
        data: FinancialData,
        input: FinancialPlanningTableInput,
        current: FinancialPlanningTable?,
    ): FinancialError? {
        if (input.assetId !in assets) return FinancialError.Validation("table.assetId", "The asset is not configured for this session")
        if (input.openingAvailable.assetId != input.assetId) return FinancialError.Validation("openingAvailable", "Opening funds must use the table asset")
        if (input.savingsPolicy != null) {
            if (input.savingsPolicy.reserve.assetId != input.assetId) return FinancialError.Validation("savingsPolicy.reserve", "The reserve must use the table asset")
            if (input.savingsPolicy.reserve.units < 0) return FinancialError.Validation("savingsPolicy.reserve", "The reserve cannot be negative")
            if (input.savingsPolicy.allocationBasisPoints !in 0..10_000) return FinancialError.Validation("savingsPolicy.allocationBasisPoints", "The allocation rate must be between 0 and 10,000 basis points")
        }
        if (input.rows.size > config.maxPlanningRows) return FinancialError.Validation("rows", "The planning table row limit has been reached")
        if (input.rows.map { it.id }.toSet().size != input.rows.size) return FinancialError.Validation("rows", "Planning row IDs must be unique")
        if (input.rows.map { it.categoryId }.toSet().size != input.rows.size) return FinancialError.Validation("rows", "A category can appear only once in a monthly table")
        val existingIds = current?.input?.rows?.map { it.id }?.toSet().orEmpty()
        for (row in input.rows) {
            if (!validId(row.id)) return FinancialError.Validation("rows.id", "A valid planning row ID is required")
            if (row.id in data.usedPlanningRowIds && row.id !in existingIds) return FinancialError.Validation("rows.id", "Planning row IDs cannot be reused")
            if (row.categoryId.isBlank()) return FinancialError.Validation("rows.categoryId", "A category is required")
            if (row.plannedAmount.assetId != input.assetId) return FinancialError.Validation("rows.plannedAmount", "Planned amounts must use the table asset")
            if (row.plannedAmount.units < 0) return FinancialError.Validation("rows.plannedAmount", "A planned amount cannot be negative")
            val category = data.categories[row.categoryId]
                ?: return FinancialError.NotFound("category", row.categoryId)
            val retainedArchived = row.id in existingIds && current?.input?.rows?.firstOrNull { it.id == row.id }?.categoryId == row.categoryId
            if (category.archived && !retainedArchived) return FinancialError.Validation("rows.categoryId", "Archived categories cannot be added to a plan")
        }
        return null
    }

    private fun deletePlanning(data: FinancialData, revision: Long, command: DeletePlanningTable): FinancialReduction {
        val current = data.planningTables[command.id]
            ?: return FinancialReduction.Rejected(FinancialError.NotFound("planning table", command.id))
        if (current.version != command.expectedVersion) {
            return FinancialReduction.Rejected(FinancialError.PlanningConflict(command.expectedVersion, current))
        }
        val period = calendar.reportingPeriod(current.input.month, config.reportingTimeZone)
        val view = calculatePlanningTableView(current, data.operations.values, data.categories.values, period, checkedNextVersion(revision))
        return accepted(data.copy(planningTables = data.planningTables.removing(current.input.id)), FinancialValue.PlanningTable(current, view))
    }

    private fun validId(id: String): Boolean = id.isNotBlank() && id.length <= 128

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
