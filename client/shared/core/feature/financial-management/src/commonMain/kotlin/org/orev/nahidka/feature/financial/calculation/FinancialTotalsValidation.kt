package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialPlanningTable
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.ReportingPeriod

internal fun validateFinancialTotals(
    operations: Iterable<FinancialOperation>,
    table: FinancialPlanningTable?,
    period: ReportingPeriod,
    assetIdentifier: String,
) {
    var income = 0L
    var expenses = 0L
    var refunds = 0L
    val expensesByCategory = if (table != null) mutableMapOf<String?, Long>() else null
    val refundsByCategory = if (table != null) mutableMapOf<String?, Long>() else null

    for (operation in operations) {
        if (operation.amount.assetIdentifier != assetIdentifier
            || operation.occurredAt < period.startInclusive
            || operation.occurredAt >= period.endExclusive
        ) {
            continue
        }

        when (operation.kind) {
            OperationKind.INCOME -> income = checkedAdd(income, operation.amount.units)
            OperationKind.EXPENSE -> {
                expenses = checkedAdd(expenses, operation.amount.units)
                expensesByCategory?.let {
                    it[operation.categoryIdentifier] =
                        checkedAdd(it[operation.categoryIdentifier] ?: 0, operation.amount.units)
                }
            }

            OperationKind.REFUND -> {
                refunds = checkedAdd(refunds, operation.amount.units)
                refundsByCategory?.let {
                    it[operation.categoryIdentifier] =
                        checkedAdd(it[operation.categoryIdentifier] ?: 0, operation.amount.units)
                }
            }
        }
    }

    val netExpenses = checkedSubtract(expenses, refunds)

    if (table == null) {
        return
    }

    val availableFunds = checkedAdd(table.openingAvailable.units, income)
    val currentAvailable = checkedSubtract(availableFunds, netExpenses)
    var includedBudget = 0L
    var reservation = 0L

    for (row in table.rows) {
        if (!row.included) {
            continue
        }

        includedBudget = checkedAdd(includedBudget, row.plannedAmount.units)

        val netSpent = checkedSubtract(
            expensesByCategory?.get(row.categoryIdentifier) ?: 0,
            refundsByCategory?.get(row.categoryIdentifier) ?: 0
        ).coerceAtLeast(0)

        val remaining = if (row.plannedAmount.units > netSpent) {
            row.plannedAmount.units - netSpent
        } else {
            0
        }

        reservation = checkedAdd(reservation, remaining)
    }
    checkedSubtract(currentAvailable, reservation)
}
