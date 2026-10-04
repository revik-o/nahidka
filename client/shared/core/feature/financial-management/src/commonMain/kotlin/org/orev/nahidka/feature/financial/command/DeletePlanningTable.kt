package org.orev.nahidka.feature.financial.command

data class DeletePlanningTable(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
)
