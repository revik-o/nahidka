package org.orev.nahidka.feature.financial.dto

sealed interface MutationResult<out T> {
    data class Committed<T>(
        val value: T,
        val storeRevision: Long,
        val changed: Boolean,
    ) : MutationResult<T>

    data class Rejected(val error: FinancialError) : MutationResult<Nothing>
}
