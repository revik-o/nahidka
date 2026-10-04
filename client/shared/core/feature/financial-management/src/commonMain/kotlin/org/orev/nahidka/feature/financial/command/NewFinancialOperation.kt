package org.orev.nahidka.feature.financial.command

import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import kotlin.time.Instant

data class NewFinancialOperation(
    val identifier: String,
    val amount: Money,
    val kind: OperationKind,
    val categoryIdentifier: String?,
    val paymentMethod: PaymentMethod,
    val occurredAt: Instant,
    val description: String? = null,
    val refundOfOperationIdentifier: String? = null,
)
