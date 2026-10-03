package org.orev.nahidka.feature.financial.command

data class UpdateFinancialOperation(
    val meta: CommandMeta,
    val id: String,
    val expectedVersion: Long,
    val patch: FinancialOperationPatch,
)
