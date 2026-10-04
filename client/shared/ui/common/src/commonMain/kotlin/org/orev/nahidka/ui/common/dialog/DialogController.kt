package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialogController<Draft, Rejection : Any>(
    private val coroutineScope: CoroutineScope,
    private val draftValidation: (Draft) -> Boolean = { true },
    private val submission: suspend (Draft) -> Rejection?,
) {

    private val mutableDialogState = MutableStateFlow<DialogState<Draft, Rejection>?>(null)

    val dialogState: StateFlow<DialogState<Draft, Rejection>?> = mutableDialogState.asStateFlow()

    fun open(draft: Draft) {
        mutableDialogState.value = DialogState(draft, draftValidation(draft))
    }

    fun edit(draftTransformation: (Draft) -> Draft) {
        mutableDialogState.update { openedDialogState ->
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
            val rejection = submission(submittedDialogState.draft)

            if (rejection == null) {
                dismiss()
            } else {
                mutableDialogState.update { openedDialogState ->
                    openedDialogState?.copy(submitting = false, rejection = rejection)
                }
            }
        }
    }

    fun dismiss() {
        mutableDialogState.value = null
    }
}
