package org.orev.nahidka.feature.financial.command

data class SavePlanningTable(
    val meta: CommandMeta,
    val expectedVersion: Long?,
    val table: FinancialPlanningTableInput,
)
