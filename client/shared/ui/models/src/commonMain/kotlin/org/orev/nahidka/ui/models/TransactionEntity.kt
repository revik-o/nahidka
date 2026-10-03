package org.orev.nahidka.ui.models

data class TransactionEntity(
    val id: String,
    val version: Long,
    val title: String,
    val formattedAmount: String,
    val isOutflow: Boolean,
    val iconName: String,
)
