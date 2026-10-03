package org.orev.nahidka.feature.financial.store

internal data class RetainedCommandReceipt(
    val command: FinancialCommand,
    val receipt: CommandReceipt,
)
