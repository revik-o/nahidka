package org.orev.nahidka.feature.financial.command

data class AddFinancialOperation(
    val meta: CommandMeta,
    val operation: NewFinancialOperation,
)
