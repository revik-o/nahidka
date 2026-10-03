package org.orev.nahidka.feature.financial.calculation

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentActuals
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningConfigured
import org.orev.nahidka.feature.financial.dto.PlanningNotConfigured
import org.orev.nahidka.feature.financial.dto.PlanningRowView
import org.orev.nahidka.feature.financial.dto.PlanningTableView
import org.orev.nahidka.feature.financial.dto.PlanningTotals
import org.orev.nahidka.feature.financial.dto.ReportingPeriod
import org.orev.nahidka.feature.financial.dto.ShareDivision
import org.orev.nahidka.feature.financial.dto.SpendingSlice
import org.orev.nahidka.feature.financial.dto.SpendingSummary
import org.orev.nahidka.feature.financial.store.InternalFrame

private const val UNCATEGORIZED_ID = "projection:uncategorized"
private const val OTHER_ID = "projection:other"
private const val OTHER_LABEL = "Other"

fun calculateSpendingSummary(
    operations: Iterable<FinancialOperation>,
    categories: Iterable<FinancialCategory>,
    period: ReportingPeriod,
    assetId: String,
    storeRevision: Long,
    maxSlices: Int = 8,
): SpendingSummary {
    require(maxSlices >= 2)
    val categoryNames = categories.associate { it.id to it.name }
    val netByCategory = mutableMapOf<String, Long>()
    var grossExpense = 0L
    var refunds = 0L
    for (operation in operations) {
        if (operation.amount.assetId != assetId) continue
        if (operation.occurredAt < period.startInclusive || operation.occurredAt >= period.endExclusive) continue
        val categoryId = operation.categoryId ?: UNCATEGORIZED_ID
        when (operation.kind) {
            OperationKind.INCOME -> Unit
            OperationKind.EXPENSE -> {
                grossExpense = checkedAdd(grossExpense, operation.amount.units)
                netByCategory[categoryId] = checkedAdd(netByCategory[categoryId] ?: 0L, operation.amount.units)
            }
            OperationKind.REFUND -> {
                refunds = checkedAdd(refunds, operation.amount.units)
                netByCategory[categoryId] = checkedSubtract(netByCategory[categoryId] ?: 0L, operation.amount.units)
            }
        }
    }
    val netExpense = checkedSubtract(grossExpense, refunds)
    var drawableTotal = 0L
    var refundCredits = 0L
    val positive = netByCategory.mapNotNull { (categoryId, units) ->
        when {
            units > 0 -> {
                drawableTotal = checkedAdd(drawableTotal, units)
                SpendingSlice(categoryId, categoryNames[categoryId] ?: if (categoryId == UNCATEGORIZED_ID) "Uncategorized" else categoryId, Money(assetId, units), 0, false)
            }
            units < 0 -> {
                refundCredits = checkedAdd(refundCredits, checkedSubtract(0L, units))
                null
            }
            else -> null
        }
    }.sortedWith(compareByDescending<SpendingSlice> { it.amount.units }.thenBy { it.categoryId })
    val visible = if (positive.size > maxSlices) {
        val regular = positive.take(maxSlices - 1)
        val otherUnits = positive.drop(maxSlices - 1).fold(0L) { total, slice -> checkedAdd(total, slice.amount.units) }
        regular + SpendingSlice(OTHER_ID, OTHER_LABEL, Money(assetId, otherUnits), 0, true)
    } else positive
    val divisions = visible.map { slice -> slice to divideShare(slice.amount.units, drawableTotal) }
    val floorTotal = divisions.sumOf { it.second.basisPointsFloor }
    val pointsRemaining = 10_000 - floorTotal
    val winners = divisions.sortedWith(
        compareByDescending<Pair<SpendingSlice, ShareDivision>> { it.second.remainder }
            .thenBy { it.first.categoryId },
    ).take(pointsRemaining).map { it.first.categoryId }.toSet()
    val slices = divisions.map { (slice, division) ->
        slice.copy(percentageBasisPoints = division.basisPointsFloor + if (slice.categoryId in winners) 1 else 0)
    }.toPersistentList()
    return SpendingSummary(
        storeRevision = storeRevision,
        period = period,
        assetId = assetId,
        grossExpense = Money(assetId, grossExpense),
        refunds = Money(assetId, refunds),
        netExpense = Money(assetId, netExpense),
        drawableTotal = Money(assetId, drawableTotal),
        refundCredits = Money(assetId, refundCredits),
        slices = slices,
    )
}

fun calculatePlanningTableView(
    table: FinancialPlanningTable,
    operations: Iterable<FinancialOperation>,
    categories: Iterable<FinancialCategory>,
    period: ReportingPeriod,
    storeRevision: Long,
): PlanningTableView {
    val input = table.input
    val categoryNames = categories.associate { it.id to it.name }
    val selectedOperations = operations.filter {
        it.amount.assetId == input.assetId &&
            it.occurredAt >= period.startInclusive &&
            it.occurredAt < period.endExclusive
    }
    val rows = input.rows.map { row ->
        val rowOperations = selectedOperations.filter { it.categoryId == row.categoryId }
        var gross = 0L
        var refunded = 0L
        val methodGross = mutableMapOf<PaymentMethod, Long>()
        val methodRefunded = mutableMapOf<PaymentMethod, Long>()
        for (operation in rowOperations) {
            when (operation.kind) {
                OperationKind.INCOME -> Unit
                OperationKind.EXPENSE -> {
                    gross = checkedAdd(gross, operation.amount.units)
                    methodGross[operation.paymentMethod] = checkedAdd(methodGross[operation.paymentMethod] ?: 0L, operation.amount.units)
                }
                OperationKind.REFUND -> {
                    refunded = checkedAdd(refunded, operation.amount.units)
                    methodRefunded[operation.paymentMethod] = checkedAdd(methodRefunded[operation.paymentMethod] ?: 0L, operation.amount.units)
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
                grossSpent = Money(input.assetId, methodSpent),
                refunded = Money(input.assetId, methodRefundedAmount),
                netSpent = Money(input.assetId, checkedSubtract(methodSpent, methodRefundedAmount)),
            )
        }.toPersistentMap()
        PlanningRowView(
            input = row,
            categoryName = categoryNames[row.categoryId] ?: "Archived category",
            grossSpent = Money(input.assetId, gross),
            refunded = Money(input.assetId, refunded),
            netSpent = Money(input.assetId, net),
            remainingBudget = Money(input.assetId, remaining),
            overspent = Money(input.assetId, overspent),
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
    val currentAvailable = checkedSubtract(checkedAdd(input.openingAvailable.units, income), checkedSubtract(grossExpenses, refunds))
    val includedRows = rows.filter { it.input.included }
    val includedBudget = includedRows.fold(0L) { total, row -> checkedAdd(total, row.input.plannedAmount.units) }
    val remainingReservation = includedRows.fold(0L) { total, row -> checkedAdd(total, row.remainingBudget.units) }
    val projectedAfterPlanning = checkedSubtract(currentAvailable, remainingReservation)
    val savingsPolicy = input.savingsPolicy
    val suggestedSavings = savingsPolicy?.let { policy ->
        val allocatable = if (projectedAfterPlanning <= policy.reserve.units) 0L else projectedAfterPlanning - policy.reserve.units
        allocateBasisPoints(allocatable, policy.allocationBasisPoints)
    }
    val rowCategories = input.rows.map { it.categoryId }.toSet()
    var unplannedNetExpense = 0L
    for (operation in selectedOperations) {
        if (operation.categoryId in rowCategories) continue
        when (operation.kind) {
            OperationKind.INCOME -> Unit
            OperationKind.EXPENSE -> unplannedNetExpense = checkedAdd(unplannedNetExpense, operation.amount.units)
            OperationKind.REFUND -> unplannedNetExpense = checkedSubtract(unplannedNetExpense, operation.amount.units)
        }
    }
    val totals = PlanningTotals(
        fundsAvailableThisMonth = Money(input.assetId, checkedAdd(input.openingAvailable.units, income)),
        grossExpenses = Money(input.assetId, grossExpenses),
        refunds = Money(input.assetId, refunds),
        currentAvailable = Money(input.assetId, currentAvailable),
        includedBudget = Money(input.assetId, includedBudget),
        remainingReservation = Money(input.assetId, remainingReservation),
        projectedAvailableAfterPlanning = Money(input.assetId, projectedAfterPlanning),
        unplannedNetExpense = Money(input.assetId, unplannedNetExpense),
        suggestedSavings = suggestedSavings?.let { Money(input.assetId, it) },
    )
    return PlanningTableView(table, storeRevision, rows, totals)
}

internal fun calculateFinancialSnapshot(
    frame: InternalFrame,
    query: MonthlyQuery,
    config: FinancialSessionConfig,
    calendar: FinancialCalendar,
): FinancialSnapshot {
    check(!frame.closed) { "Financial session is closed" }
    val asset = config.assets.firstOrNull { it.id == query.assetId }
        ?: throw IllegalArgumentException("Asset ${query.assetId} is not configured for this session")
    val period = calendar.reportingPeriod(query.month, config.reportingTimeZone)
    val operations = frame.data.operations.values
        .asSequence()
        .filter { it.amount.assetId == query.assetId && it.occurredAt >= period.startInclusive && it.occurredAt < period.endExclusive }
        .sortedWith(compareByDescending<FinancialOperation> { it.occurredAt }.thenBy { it.id })
        .toPersistentList()
    val categories = frame.data.categories.values.sortedWith(compareBy<FinancialCategory> { it.name.lowercase() }.thenBy { it.id }).toPersistentList()
    val spending = calculateSpendingSummary(operations, categories, period, query.assetId, frame.revision)
    val table = frame.data.planningTables.values.firstOrNull { it.input.month == query.month && it.input.assetId == query.assetId }
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
            income = Money(query.assetId, income),
            grossExpenses = Money(query.assetId, grossExpenses),
            refunds = Money(query.assetId, refunds),
            netExpense = Money(query.assetId, checkedSubtract(grossExpenses, refunds)),
        )
    } else {
        PlanningConfigured(calculatePlanningTableView(table, operations, categories, period, frame.revision))
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
