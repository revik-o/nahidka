package org.orev.nahidka.feature.financial.command

import kotlin.time.Instant
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class FinancialOperationPatch(
    val amount: Money? = null,
    val kind: OperationKind? = null,
    val categoryId: NullablePatch<String> = NullablePatch.Keep,
    val paymentMethod: PaymentMethod? = null,
    val occurredAt: Instant? = null,
    val description: NullablePatch<String> = NullablePatch.Keep,
    val refundOfOperationId: NullablePatch<String> = NullablePatch.Keep,
)
