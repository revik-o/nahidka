package org.orev.nahidka.feature.financial.command

data class RemoveFinancialOperation(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
)
