package org.orev.nahidka.feature.financial.dto

data class FinancialCategory(
    val id: String,
    val version: Long,
    val name: String,
    val iconName: String?,
    val archived: Boolean,
)
