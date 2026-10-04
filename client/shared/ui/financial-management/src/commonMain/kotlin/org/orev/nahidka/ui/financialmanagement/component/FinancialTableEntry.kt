package org.orev.nahidka.ui.financialmanagement.component

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.PaymentMethod

interface FinancialTableEntry {
    val categoryIconName: String?
    val categoryName: String
    val formattedAmount: String
    val amountDirection: FinancialAmountDirection?
    val paymentMethod: PaymentMethod?
    val occurredOn: LocalDate?
}
