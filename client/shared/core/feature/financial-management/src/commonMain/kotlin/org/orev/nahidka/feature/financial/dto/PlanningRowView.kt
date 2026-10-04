package org.orev.nahidka.feature.financial.dto

import kotlinx.collections.immutable.PersistentMap

data class PlanningRowView(
    val input: PlanningRow,
    val categoryName: String,
    val grossSpent: Money,
    val refunded: Money,
    val netSpent: Money,
    val remainingBudget: Money,
    val overspent: Money,
    val actualByPaymentMethod: PersistentMap<PaymentMethod, PaymentActuals>,
)
