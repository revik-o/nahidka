package org.orev.nahidka.ui.financialmanagement.planning

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.PlanningRow
import org.orev.nahidka.ui.financialmanagement.component.FinancialAmountDirection
import org.orev.nahidka.ui.financialmanagement.component.FinancialTableEntry

data class FinancialPlanningRow(
    val planningRow: PlanningRow,
    val category: FinancialCategory?,
    override val categoryName: String,
    override val formattedAmount: String,
) : FinancialTableEntry {

    override val categoryIconName: String? get() = category?.iconName

    override val amountDirection: FinancialAmountDirection? get() = null

    override val paymentMethod: PaymentMethod? get() = planningRow.preferredPaymentMethod

    override val occurredOn: LocalDate? get() = null
}
