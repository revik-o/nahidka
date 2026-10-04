package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

data class FinancialOperationDraft(
    val editedOperation: FinancialOperation?,
    val asset: AssetDefinition,
    val kind: OperationKind,
    val amountText: String,
    val category: FinancialCategory?,
    val paymentMethod: PaymentMethod,
    val occurredOn: LocalDate,
) {

    val amount: Money? get() = parsePositiveMoney(amountText, asset)

    val submittable: Boolean get() = amount != null && category != null
}
