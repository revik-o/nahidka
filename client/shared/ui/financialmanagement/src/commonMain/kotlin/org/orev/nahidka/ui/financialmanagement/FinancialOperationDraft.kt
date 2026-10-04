package org.orev.nahidka.ui.financialmanagement

import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class FinancialOperationDraft(
    val original: FinancialOperation?,
    val amountText: String,
    val assetIdentifier: String,
    val kind: OperationKind,
    val categoryIdentifier: String?,
    val paymentMethod: PaymentMethod,
    val occurredAtInstant: kotlin.time.Instant,
    val descriptionText: String,
    val refundOfOperationIdentifier: String?,
)
