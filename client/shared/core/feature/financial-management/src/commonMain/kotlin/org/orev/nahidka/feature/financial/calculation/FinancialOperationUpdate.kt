package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.core.common.applyTo
import org.orev.nahidka.feature.financial.dto.FinancialOperation

internal fun FinancialOperation.apply(patch: FinancialOperationPatch): FinancialOperation = copy(
    amount = patch.amount ?: amount,
    categoryIdentifier = patch.categoryIdentifier.applyTo(categoryIdentifier),
    paymentMethod = patch.paymentMethod ?: paymentMethod,
    occurredAt = patch.occurredAt ?: occurredAt,
    description = patch.description.applyTo(description),
    refundOfOperationIdentifier = patch.refundOfOperationIdentifier.applyTo(refundOfOperationIdentifier),
)
