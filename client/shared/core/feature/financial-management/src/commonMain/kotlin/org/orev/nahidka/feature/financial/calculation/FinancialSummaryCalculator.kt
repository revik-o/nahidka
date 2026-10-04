package org.orev.nahidka.feature.financial.calculation

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.store.InternalFrame

private const val UNCATEGORIZED_IDENTIFIER = "projection:uncategorized"
private const val OTHER_IDENTIFIER = "projection:other"
private const val OTHER_LABEL = "Other"

fun calculateSpendingSummary(
    operations: Iterable<FinancialOperation>,
    categories: Iterable<FinancialCategory>,
    period: ReportingPeriod,
    assetIdentifier: String,
    storeRevision: Long,
    maxSlices: Int = 8,
): SpendingSummary {
    require(maxSlices >= 2)

    val categoryNames = categories.associate { it.identifier to it.name }
    val netByCategory = mutableMapOf<String, Long>()
    var grossExpense = 0L
    var refunds = 0L

    for (operation in operations) {
        if (operation.amount.assetIdentifier != assetIdentifier) {
            continue
        }

        if (operation.occurredAt < period.startInclusive || operation.occurredAt >= period.endExclusive) {
            continue
        }

        val categoryIdentifier = operation.categoryIdentifier ?: UNCATEGORIZED_IDENTIFIER

        when (operation.kind) {
            OperationKind.INCOME -> Unit
            OperationKind.EXPENSE -> {
                grossExpense = checkedAdd(grossExpense, operation.amount.units)
                netByCategory[categoryIdentifier] =
                    checkedAdd(netByCategory[categoryIdentifier] ?: 0L, operation.amount.units)
            }

            OperationKind.REFUND -> {
                refunds = checkedAdd(refunds, operation.amount.units)
                netByCategory[categoryIdentifier] =
                    checkedSubtract(netByCategory[categoryIdentifier] ?: 0L, operation.amount.units)
            }
        }
    }

    val netExpense = checkedSubtract(grossExpense, refunds)
    var drawableTotal = 0L
    var refundCredits = 0L

    val positive = netByCategory.mapNotNull { (categoryIdentifier, units) ->
        when {
            units > 0 -> {
                drawableTotal = checkedAdd(drawableTotal, units)
                SpendingSlice(
                    categoryIdentifier,
                    categoryNames[categoryIdentifier]
                        ?: if (categoryIdentifier == UNCATEGORIZED_IDENTIFIER) "Uncategorized" else categoryIdentifier,
                    Money(assetIdentifier, units),
                    0,
                    false
                )
            }

            units < 0 -> {
                refundCredits = checkedAdd(refundCredits, checkedSubtract(0L, units))
                null
            }

            else -> null
        }
    }.sortedWith(compareByDescending<SpendingSlice> { it.amount.units }.thenBy { it.categoryIdentifier })

    val visible = if (positive.size > maxSlices) {
        val regular = positive.take(maxSlices - 1)
        val otherUnits = positive.drop(maxSlices - 1).fold(0L) { total, slice -> checkedAdd(total, slice.amount.units) }
        regular + SpendingSlice(OTHER_IDENTIFIER, OTHER_LABEL, Money(assetIdentifier, otherUnits), 0, true)
    } else positive

    val divisions = visible.map { slice -> slice to divideShare(slice.amount.units, drawableTotal) }
    val floorTotal = divisions.sumOf { it.second.basisPointsFloor }
    val pointsRemaining = 10_000 - floorTotal

    val winners = divisions.sortedWith(
        compareByDescending<Pair<SpendingSlice, ShareDivision>> { it.second.remainder }
            .thenBy { it.first.categoryIdentifier },
    ).take(pointsRemaining).map { it.first.categoryIdentifier }.toSet()

    val slices = divisions.map { (slice, division) ->
        slice.copy(percentageBasisPoints = division.basisPointsFloor + if (slice.categoryIdentifier in winners) 1 else 0)
    }.toPersistentList()

    return SpendingSummary(
        storeRevision = storeRevision,
        period = period,
        assetIdentifier = assetIdentifier,
        grossExpense = Money(assetIdentifier, grossExpense),
        refunds = Money(assetIdentifier, refunds),
        netExpense = Money(assetIdentifier, netExpense),
        drawableTotal = Money(assetIdentifier, drawableTotal),
        refundCredits = Money(assetIdentifier, refundCredits),
        slices = slices,
    )
}

fun calculatePlanningTableView(
    table: FinancialPlanningTable,
    operations: Iterable<FinancialOperation>,
    categories: Iterable<FinancialCategory>,
    period: ReportingPeriod,
): PlanningTableView {
    val input = table

    val categoryNames = categories.associate {
        it.identifier to it.name
    }

    val selectedOperations = operations.filter {
        it.amount.assetIdentifier == input.assetIdentifier &&
                it.occurredAt >= period.startInclusive &&
                it.occurredAt < period.endExclusive
    }

    val operationsByCategoryIdentifier = selectedOperations.groupBy {
        it.categoryIdentifier
    }

    val rows = input.rows.map { row ->
        val rowOperations = operationsByCategoryIdentifier[row.categoryIdentifier].orEmpty()
        var gross = 0L
        var refunded = 0L
        val methodGross = mutableMapOf<PaymentMethod, Long>()
        val methodRefunded = mutableMapOf<PaymentMethod, Long>()

        for (operation in rowOperations) {
            when (operation.kind) {
                OperationKind.INCOME -> Unit
                OperationKind.EXPENSE -> {
                    gross = checkedAdd(gross, operation.amount.units)
                    methodGross[operation.paymentMethod] =
                        checkedAdd(methodGross[operation.paymentMethod] ?: 0L, operation.amount.units)
                }

                OperationKind.REFUND -> {
                    refunded = checkedAdd(refunded, operation.amount.units)
                    methodRefunded[operation.paymentMethod] =
                        checkedAdd(methodRefunded[operation.paymentMethod] ?: 0L, operation.amount.units)
                }
            }
        }

        val net = checkedSubtract(gross, refunded)
        val spentForBudget = net.coerceAtLeast(0L)
        val remaining = if (row.plannedAmount.units > spentForBudget) row.plannedAmount.units - spentForBudget else 0L
        val overspent = if (spentForBudget > row.plannedAmount.units) spentForBudget - row.plannedAmount.units else 0L
        val methods = PaymentMethod.entries.associateWith { method ->
            val methodSpent = methodGross[method] ?: 0L
            val methodRefundedAmount = methodRefunded[method] ?: 0L

            PaymentActuals(
                grossSpent = Money(input.assetIdentifier, methodSpent),
                refunded = Money(input.assetIdentifier, methodRefundedAmount),
                netSpent = Money(input.assetIdentifier, checkedSubtract(methodSpent, methodRefundedAmount)),
            )
        }.toPersistentMap()

        PlanningRowView(
            input = row,
            categoryName = categoryNames[row.categoryIdentifier] ?: "Archived category",
            grossSpent = Money(input.assetIdentifier, gross),
            refunded = Money(input.assetIdentifier, refunded),
            netSpent = Money(input.assetIdentifier, net),
            remainingBudget = Money(input.assetIdentifier, remaining),
            overspent = Money(input.assetIdentifier, overspent),
            actualByPaymentMethod = methods,
        )
    }.toPersistentList()

    var income = 0L
    var grossExpenses = 0L
    var refunds = 0L

    for (operation in selectedOperations) {
        when (operation.kind) {
            OperationKind.INCOME -> income = checkedAdd(income, operation.amount.units)
            OperationKind.EXPENSE -> grossExpenses = checkedAdd(grossExpenses, operation.amount.units)
            OperationKind.REFUND -> refunds = checkedAdd(refunds, operation.amount.units)
        }
    }

    val currentAvailable =
        checkedSubtract(checkedAdd(input.openingAvailable.units, income), checkedSubtract(grossExpenses, refunds))
    val includedRows = rows.filter { it.input.included }
    val includedBudget = includedRows.fold(0L) { total, row -> checkedAdd(total, row.input.plannedAmount.units) }
    val remainingReservation = includedRows.fold(0L) { total, row -> checkedAdd(total, row.remainingBudget.units) }
    val projectedAfterPlanning = checkedSubtract(currentAvailable, remainingReservation)
    val savingsPolicy = input.savingsPolicy

    val suggestedSavings = savingsPolicy?.let { policy ->
        val allocatable = if (projectedAfterPlanning <= policy.reserve.units) {
            0L
        } else {
            projectedAfterPlanning - policy.reserve.units
        }

        allocateBasisPoints(allocatable, policy.allocationBasisPoints)
    }

    val rowCategories = input.rows.map { it.categoryIdentifier }.toSet()
    var unplannedNetExpense = 0L

    for (operation in selectedOperations) {
        if (operation.categoryIdentifier in rowCategories) {
            continue
        }

        when (operation.kind) {
            OperationKind.INCOME -> Unit
            OperationKind.EXPENSE -> unplannedNetExpense = checkedAdd(unplannedNetExpense, operation.amount.units)
            OperationKind.REFUND -> unplannedNetExpense = checkedSubtract(unplannedNetExpense, operation.amount.units)
        }
    }

    val totals = PlanningTotals(
        fundsAvailableThisMonth = Money(input.assetIdentifier, checkedAdd(input.openingAvailable.units, income)),
        grossExpenses = Money(input.assetIdentifier, grossExpenses),
        refunds = Money(input.assetIdentifier, refunds),
        currentAvailable = Money(input.assetIdentifier, currentAvailable),
        includedBudget = Money(input.assetIdentifier, includedBudget),
        remainingReservation = Money(input.assetIdentifier, remainingReservation),
        projectedAvailableAfterPlanning = Money(input.assetIdentifier, projectedAfterPlanning),
        unplannedNetExpense = Money(input.assetIdentifier, unplannedNetExpense),
        suggestedSavings = suggestedSavings?.let { Money(input.assetIdentifier, it) },
    )

    return PlanningTableView(table, rows, totals)
}

internal fun calculateFinancialSnapshot(
    frame: InternalFrame,
    query: MonthlyQuery,
    config: FinancialSessionConfig,
    calendar: FinancialCalendar,
): FinancialSnapshot {
    check(!frame.closed) {
        "Financial session is closed"
    }

    val asset = config.assets.firstOrNull {
        it.identifier == query.assetIdentifier
    } ?: throw IllegalArgumentException("Asset ${query.assetIdentifier} is not configured for this session")

    val period = calendar.reportingPeriod(query.month, config.reportingTimeZone)
    val operations = frame.data.operations.values
        .asSequence()
        .filter { it.amount.assetIdentifier == query.assetIdentifier && it.occurredAt >= period.startInclusive && it.occurredAt < period.endExclusive }
        .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.identifier })
        .toPersistentList()

    val categories = frame.data.categories.values.sortedWith(FinancialCategoryNameComparator).toPersistentList()
    val spending = calculateSpendingSummary(operations, categories, period, query.assetIdentifier, frame.revision)
    val table = frame.data.planningTables.values.firstOrNull {
        it.month == query.month && it.assetIdentifier == query.assetIdentifier
    }

    val planning = if (table == null) {
        var income = 0L
        var grossExpenses = 0L
        var refunds = 0L

        for (operation in operations) {
            when (operation.kind) {
                OperationKind.INCOME -> income = checkedAdd(income, operation.amount.units)
                OperationKind.EXPENSE -> grossExpenses = checkedAdd(grossExpenses, operation.amount.units)
                OperationKind.REFUND -> refunds = checkedAdd(refunds, operation.amount.units)
            }
        }

        PlanningNotConfigured(
            period = period,
            income = Money(query.assetIdentifier, income),
            grossExpenses = Money(query.assetIdentifier, grossExpenses),
            refunds = Money(query.assetIdentifier, refunds),
            netExpense = Money(query.assetIdentifier, checkedSubtract(grossExpenses, refunds)),
        )
    } else {
        PlanningConfigured(calculatePlanningTableView(table, operations, categories, period))
    }

    return FinancialSnapshot(
        sessionIdentity = frame.sessionIdentity,
        storeRevision = frame.revision,
        period = period,
        asset = asset,
        operations = operations,
        categories = categories,
        spending = spending,
        planning = planning,
    )
}

fun financialMonthFor(operation: FinancialOperation, reportingTimeZone: String): YearMonth =
    operation.occurredAt.toLocalDateTime(TimeZone.of(reportingTimeZone)).date.let { date ->
        YearMonth(date.year, date.month.ordinal + 1)
    }
