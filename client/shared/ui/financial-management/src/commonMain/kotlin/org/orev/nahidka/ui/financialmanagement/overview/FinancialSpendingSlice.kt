package org.orev.nahidka.ui.financialmanagement.overview

data class FinancialSpendingSlice(
    val label: String,
    val formattedAmount: String,
    val formattedPercentage: String,
    val sweepFraction: Float,
)
