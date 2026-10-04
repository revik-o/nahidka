package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.FinancialError

class FinancialDialogController<Draft> internal constructor(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
    private val mutation: suspend (Draft) -> MutationResult<*>,
) {

    private val mutableDialogState = MutableStateFlow<FinancialDialogState<Draft>?>(null)

    val dialogState: StateFlow<FinancialDialogState<Draft>?> = mutableDialogState.asStateFlow()

    fun open(draft: Draft) {
        if (mutableDialogState.value?.submitting == true) return
        mutableDialogState.value = FinancialDialogState(draft, draftValidation(draft))
    }

    fun edit(draftTransformation: (Draft) -> Draft) {
        mutableDialogState.update { openedDialogState ->
            if (openedDialogState?.submitting == true) return@update openedDialogState
            openedDialogState?.let { dialogState ->
                val editedDraft = draftTransformation(dialogState.draft)

                dialogState.copy(draft = editedDraft, submittable = draftValidation(editedDraft), rejection = null)
            }
        }
    }

    fun submit() {
        val submittedDialogState = mutableDialogState.value

        if (submittedDialogState == null || !submittedDialogState.submittable || submittedDialogState.submitting) {
            return
        }

        mutableDialogState.value = submittedDialogState.copy(submitting = true, rejection = null)

        coroutineScope.launch {
            val mutationResult = try {
                mutation(submittedDialogState.draft)
            } catch (cancelled: CancellationException) {
                mutableDialogState.update { openedDialogState -> openedDialogState?.copy(submitting = false) }
                throw cancelled
            } catch (failure: Exception) {
                MutationResult.Rejected(FinancialError.StorageUnavailable)
            }
            when (mutationResult) {
                is MutationResult.Committed -> mutableDialogState.value = null
                is MutationResult.Rejected -> mutableDialogState.update { openedDialogState ->
                    openedDialogState?.copy(submitting = false, rejection = mutationResult.error)
                }
            }
        }
    }

    fun dismiss() {
        if (mutableDialogState.value?.submitting == true) return
        mutableDialogState.value = null
    }
}
