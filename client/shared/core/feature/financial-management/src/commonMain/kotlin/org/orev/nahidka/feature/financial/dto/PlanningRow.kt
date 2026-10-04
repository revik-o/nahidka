package org.orev.nahidka.feature.financial.dto

import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.PaymentMethod

data class PlanningRow(
    val identifier: String,
    val categoryIdentifier: String,
    val plannedAmount: Money,
    val included: Boolean,
    val preferredPaymentMethod: PaymentMethod?,
)
