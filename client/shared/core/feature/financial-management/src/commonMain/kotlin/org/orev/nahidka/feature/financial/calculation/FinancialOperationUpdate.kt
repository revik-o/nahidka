package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.applyTo
import org.orev.nahidka.feature.financial.dto.FinancialOperation

internal fun FinancialOperation.apply(patch: FinancialOperationPatch): FinancialOperation = copy(
    amount = patch.amount ?: amount,
    kind = patch.kind ?: kind,
    categoryId = patch.categoryId.applyTo(categoryId),
    paymentMethod = patch.paymentMethod ?: paymentMethod,
    occurredAt = patch.occurredAt ?: occurredAt,
    description = patch.description.applyTo(description),
    refundOfOperationId = patch.refundOfOperationId.applyTo(refundOfOperationId),
)
