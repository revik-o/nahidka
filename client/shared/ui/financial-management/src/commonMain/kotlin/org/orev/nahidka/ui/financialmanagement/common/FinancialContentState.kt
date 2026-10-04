package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

private const val FINANCIAL_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L

sealed interface FinancialContentState<out Content> {

    data object Loading : FinancialContentState<Nothing>

    data object Unavailable : FinancialContentState<Nothing>

    data class Available<Content>(val content: Content) : FinancialContentState<Content>
}

internal fun <Content> FinancialContentState<Content>.availableContentOrNull(): Content? =
    (this as? FinancialContentState.Available)?.content

internal fun <Content> Flow<Content>.stateInFinancialContent(
    coroutineScope: CoroutineScope,
): StateFlow<FinancialContentState<Content>> =
    this
        .map<Content, FinancialContentState<Content>> { content -> FinancialContentState.Available(content) }
        .catch { emit(FinancialContentState.Unavailable) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(FINANCIAL_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = FinancialContentState.Loading,
        )
