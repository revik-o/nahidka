package org.orev.nahidka.ui.common.dialog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DialogControllerTest {

    private val submittedDrafts = mutableListOf<String>()

    @Test
    fun successfulSubmissionClosesDialog() = runTest {
        val dialogController = dialogController(rejection = null)

        dialogController.open("Write plan")
        dialogController.submit()
        advanceUntilIdle()

        assertEquals(listOf("Write plan"), submittedDrafts)
        assertNull(dialogController.dialogState.value)
    }

    @Test
    fun rejectedSubmissionKeepsDialogOpenUntilNextEdit() = runTest {
        val dialogController = dialogController(rejection = "Rejected")

        dialogController.open("Write plan")
        dialogController.submit()
        advanceUntilIdle()
        assertEquals("Rejected", checkNotNull(dialogController.dialogState.value).rejection)

        dialogController.edit { draft -> "$draft again" }
        assertEquals(DialogState("Write plan again", submittable = true), dialogController.dialogState.value)
    }

    @Test
    fun invalidDraftIsNotSubmitted() = runTest {
        val dialogController = dialogController(rejection = null)

        dialogController.open(" ")
        dialogController.submit()
        advanceUntilIdle()

        assertTrue(submittedDrafts.isEmpty())
        assertFalse(checkNotNull(dialogController.dialogState.value).submittable)
    }

    private fun TestScope.dialogController(rejection: String?): DialogController<String, String> =
        DialogController(this, String::isNotBlank) { draft ->
            submittedDrafts.add(draft)
            rejection
        }
}
