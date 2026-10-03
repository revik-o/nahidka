package org.orev.nahidka.feature.financial.command

import kotlin.time.Instant
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class NewFinancialOperation(
    val id: String,
    val amount: Money,
    val kind: OperationKind,
    val categoryId: String?,
    val paymentMethod: PaymentMethod,
    val occurredAt: Instant,
    val description: String? = null,
    val refundOfOperationId: String? = null,
)
