package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.ui.financialmanagement.component.FinancialAmountDirection
import org.orev.nahidka.ui.financialmanagement.component.FinancialTableEntry

data class FinancialOperationRow(
    val operation: FinancialOperation,
    val category: FinancialCategory?,
    override val formattedAmount: String,
    override val occurredOn: LocalDate,
) : FinancialTableEntry {

    override val categoryIconName: String? get() = category?.iconName

    override val categoryName: String get() = category?.name.orEmpty()

    override val amountDirection: FinancialAmountDirection
        get() = when (operation.kind) {
            OperationKind.EXPENSE -> FinancialAmountDirection.OUTGOING
            OperationKind.INCOME, OperationKind.REFUND -> FinancialAmountDirection.INCOMING
        }

    override val paymentMethod: PaymentMethod get() = operation.paymentMethod
}
