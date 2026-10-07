package org.orev.nahidka.ui.financialmanagement.overview

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.calculation.checkedAdd
import org.orev.nahidka.feature.financial.dto.FinancialSnapshot
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PlanningConfigured
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter

private const val BASIS_POINTS_PER_PERCENT = 100
private const val BASIS_POINTS_PER_WHOLE = 10_000f

data class FinancialOverview(
    val month: YearMonth,
    val assetDisplayCode: String,
    val formattedNetExpense: String,
    val formattedIncome: String,
    val formattedAvailableAfterPlanning: String?,
    val formattedRefundCredits: String?,
    val spendingSlices: PersistentList<FinancialSpendingSlice>,
) {

    companion object {

        fun of(financialSnapshot: FinancialSnapshot, exactMoneyFormatter: ExactMoneyFormatter): FinancialOverview {
            val formatMoney = { money: Money -> exactMoneyFormatter.format(money, financialSnapshot.asset) }
            val incomeUnits = financialSnapshot.operations
                .filter { operation -> operation.kind == OperationKind.INCOME }
                .fold(0L) { incomeSum, operation -> checkedAdd(incomeSum, operation.amount.units) }

            return FinancialOverview(
                month = financialSnapshot.period.month,
                assetDisplayCode = financialSnapshot.asset.displayCode,
                formattedNetExpense = formatMoney(financialSnapshot.spending.netExpense),
                formattedIncome = formatMoney(Money(financialSnapshot.asset.identifier, incomeUnits)),
                formattedAvailableAfterPlanning = (financialSnapshot.planning as? PlanningConfigured)
                    ?.table
                    ?.totals
                    ?.projectedAvailableAfterPlanning
                    ?.let(formatMoney),
                formattedRefundCredits = financialSnapshot.spending.refundCredits
                    .takeIf { refundCredits -> refundCredits.units > 0 }
                    ?.let(formatMoney),
                spendingSlices = financialSnapshot.spending.slices
                    .map { spendingSlice ->
                        FinancialSpendingSlice(
                            label = spendingSlice.label,
                            formattedAmount = formatMoney(spendingSlice.amount),
                            formattedPercentage = formatPercentage(spendingSlice.percentageBasisPoints),
                            sweepFraction = spendingSlice.percentageBasisPoints / BASIS_POINTS_PER_WHOLE,
                        )
                    }
                    .toPersistentList(),
            )
        }

        private fun formatPercentage(basisPoints: Int): String {
            val fractionDigits = (basisPoints % BASIS_POINTS_PER_PERCENT)
                .toString()
                .padStart(2, '0')

            return "${basisPoints / BASIS_POINTS_PER_PERCENT}.$fractionDigits%"
        }
    }
}
