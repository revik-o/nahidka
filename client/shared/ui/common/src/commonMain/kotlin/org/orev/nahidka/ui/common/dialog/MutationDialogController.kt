package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.CoroutineScope
import org.orev.nahidka.ui.common.mutation.mutationRejectionOf

class MutationDialogController<Draft>(
    coroutineScope: CoroutineScope,
    draftValidation: (Draft) -> Boolean = { true },
    mutation: suspend (Draft) -> Unit,
) : DialogController<Draft, IllegalArgumentException>(
    coroutineScope = coroutineScope,
    draftValidation = draftValidation,
    submission = { draft -> mutationRejectionOf { mutation(draft) } },
)
