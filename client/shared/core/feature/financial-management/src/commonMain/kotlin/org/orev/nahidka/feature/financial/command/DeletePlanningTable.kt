package org.orev.nahidka.feature.financial.command

data class DeletePlanningTable(
    val meta: CommandMeta,
    val id: String,
    val expectedVersion: Long,
)
