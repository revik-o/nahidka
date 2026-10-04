package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

data class FinancialPlanningRowDraft(
    val planningTable: FinancialPlanningTable?,
    val month: YearMonth,
    val asset: AssetDefinition,
    val rowIdentifier: String,
    val category: FinancialCategory?,
    val plannedAmountText: String,
    val preferredPaymentMethod: PaymentMethod,
) {

    val creation: Boolean
        get() = planningTable?.rows.orEmpty().none { planningRow -> planningRow.identifier == rowIdentifier }

    val plannedAmount: Money? get() = parsePositiveMoney(plannedAmountText, asset)

    val submittable: Boolean get() = plannedAmount != null && category != null

    fun selectableCategories(categories: List<FinancialCategory>): List<FinancialCategory> {
        val plannedCategoryIdentifiers = planningTable?.rows.orEmpty()
            .filterNot { planningRow -> planningRow.identifier == rowIdentifier }
            .map(PlanningRow::categoryIdentifier)
            .toSet()

        return categories.filter { category ->
            !category.archived && category.identifier !in plannedCategoryIdentifiers
        }
    }
}
