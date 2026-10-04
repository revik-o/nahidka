package org.orev.nahidka.feature.financial.command

data class DeleteFinancialCategory(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
)
