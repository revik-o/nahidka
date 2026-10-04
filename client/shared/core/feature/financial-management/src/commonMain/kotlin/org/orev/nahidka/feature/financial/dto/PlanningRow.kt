package org.orev.nahidka.feature.financial.dto

data class PlanningRow(
    val identifier: String,
    val categoryIdentifier: String,
    val plannedAmount: Money,
    val included: Boolean,
    val preferredPaymentMethod: PaymentMethod?,
)
