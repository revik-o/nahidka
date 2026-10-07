package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MutationDialogControllerTest {

    @Test
    fun rejectedMutationKeepsDialogOpenWithRejection() = runTest {
        val rejection = IllegalArgumentException("Title must not be blank")
        val mutationDialogController = MutationDialogController<String>(this) { throw rejection }

        mutationDialogController.open("Write plan")
        mutationDialogController.submit()
        advanceUntilIdle()

        assertEquals(rejection, checkNotNull(mutationDialogController.dialogState.value).rejection)
    }

    @Test
    fun acceptedMutationClosesDialog() = runTest {
        val mutatedDrafts = mutableListOf<String>()
        val mutationDialogController = MutationDialogController<String>(this) { draft -> mutatedDrafts.add(draft) }

        mutationDialogController.open("Write plan")
        mutationDialogController.submit()
        advanceUntilIdle()

        assertEquals(listOf("Write plan"), mutatedDrafts)
        assertNull(mutationDialogController.dialogState.value)
    }
}
