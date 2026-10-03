package org.orev.nahidka.feature.financial.dto

data class PaymentActuals(
    val grossSpent: Money,
    val refunded: Money,
    val netSpent: Money,
)
