package org.orev.nahidka.ui.financialmanagement

import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class FinancialOperationDraft(
    val original: FinancialOperation?,
    val amountText: String,
    val assetId: String,
    val kind: OperationKind,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val occurredAtText: String,
    val descriptionText: String,
    val refundOfOperationId: String?,
)
