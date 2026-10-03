package org.orev.nahidka.feature.financial.dto

import kotlin.time.Instant

data class FinancialOperation(
    val id: String,
    val version: Long,
    val amount: Money,
    val kind: OperationKind,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val occurredAt: Instant,
    val description: String?,
    val refundOfOperationId: String?,
)
