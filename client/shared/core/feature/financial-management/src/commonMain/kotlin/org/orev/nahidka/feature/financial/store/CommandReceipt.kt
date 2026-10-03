package org.orev.nahidka.feature.financial.store

internal data class CommandReceipt(
    val value: FinancialValue,
    val storeRevision: Long,
    val changed: Boolean,
)
