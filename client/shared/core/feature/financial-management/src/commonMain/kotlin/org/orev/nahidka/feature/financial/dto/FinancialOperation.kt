package org.orev.nahidka.feature.financial.dto

import kotlin.time.Instant

data class FinancialOperation(
    val identifier: String,
    val version: Long,
    val amount: Money,
    val kind: OperationKind,
    val categoryIdentifier: String?,
    val paymentMethod: PaymentMethod,
    val occurredAt: Instant,
    val description: String?,
    val refundOfOperationIdentifier: String?,
)
