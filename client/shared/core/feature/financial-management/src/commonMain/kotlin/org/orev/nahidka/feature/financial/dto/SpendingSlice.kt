package org.orev.nahidka.feature.financial.dto

data class SpendingSlice(
    val categoryIdentifier: String,
    val label: String,
    val amount: Money,
    val percentageBasisPoints: Int,
    val isOtherGroup: Boolean,
)
