package org.orev.nahidka.feature.financial.command

data class ArchiveFinancialCategory(
    val meta: CommandMeta,
    val id: String,
    val expectedVersion: Long,
)
