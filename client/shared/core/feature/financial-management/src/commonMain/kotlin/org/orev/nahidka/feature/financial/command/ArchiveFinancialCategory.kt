package org.orev.nahidka.feature.financial.command

data class ArchiveFinancialCategory(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
)
