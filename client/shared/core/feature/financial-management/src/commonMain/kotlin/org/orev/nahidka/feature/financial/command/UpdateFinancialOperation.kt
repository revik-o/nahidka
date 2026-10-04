package org.orev.nahidka.feature.financial.command

data class UpdateFinancialOperation(
    val meta: CommandMeta,
    val identifier: String,
    val expectedVersion: Long,
    val patch: FinancialOperationPatch,
)
