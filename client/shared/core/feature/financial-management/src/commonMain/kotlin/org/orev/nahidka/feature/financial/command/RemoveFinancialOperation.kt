package org.orev.nahidka.feature.financial.command

data class RemoveFinancialOperation(
    val meta: CommandMeta,
    val id: String,
    val expectedVersion: Long,
)
