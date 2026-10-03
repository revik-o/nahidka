package org.orev.nahidka.feature.financial.command

import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class PlanningRowInput(
    val id: String,
    val categoryId: String,
    val plannedAmount: Money,
    val included: Boolean,
    val preferredPaymentMethod: PaymentMethod?,
)
