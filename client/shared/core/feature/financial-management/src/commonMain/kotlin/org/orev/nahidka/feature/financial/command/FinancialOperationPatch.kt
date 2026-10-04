package org.orev.nahidka.feature.financial.command

import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import kotlin.time.Instant

data class FinancialOperationPatch(
    val amount: Money? = null,
    val categoryIdentifier: NullablePatch<String> = NullablePatch.Keep,
    val paymentMethod: PaymentMethod? = null,
    val occurredAt: Instant? = null,
    val description: NullablePatch<String> = NullablePatch.Keep,
    val refundOfOperationIdentifier: NullablePatch<String> = NullablePatch.Keep,
)
