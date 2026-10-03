package org.orev.nahidka.feature.financial.dto

data class SavingsGuidelinePolicy(
    val reserve: Money,
    val allocationBasisPoints: Int,
)
