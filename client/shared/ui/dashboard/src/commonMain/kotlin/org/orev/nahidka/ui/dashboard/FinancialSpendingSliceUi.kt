package org.orev.nahidka.ui.dashboard

data class FinancialSpendingSliceUi(
    val categoryIdentifier: String,
    val label: String,
    val formattedAmount: String,
    val percentageBasisPoints: Int,
    val isOtherGroup: Boolean,
)
