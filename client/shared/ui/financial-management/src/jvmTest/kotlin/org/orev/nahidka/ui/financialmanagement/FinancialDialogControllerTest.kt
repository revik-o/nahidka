package org.orev.nahidka.ui.financialmanagement

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.ui.financialmanagement.common.FinancialDialogController
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class FinancialDialogControllerTest {

    @Test
    fun pendingSaveCannotBeSubmittedTwiceOrReplaceItsDraft() = runTest {
        val receipt = CompletableDeferred<MutationResult<String>>()
        var submissions = 0
        val controller = FinancialDialogController<String>(backgroundScope) { draft ->
            assertEquals("original", draft)
            submissions += 1
            receipt.await()
        }
        controller.open("original")
        controller.submit()
        controller.submit()
        controller.edit { "edited" }
        controller.dismiss()
        controller.open("replacement")
        runCurrent()
        assertEquals(1, submissions)
        assertEquals("original", controller.dialogState.value?.draft)
        assertTrue(checkNotNull(controller.dialogState.value).submitting)
        receipt.complete(MutationResult.Committed("original", 1, true))
        runCurrent()
        assertNull(controller.dialogState.value)
    }

    @Test
    fun rejectedSaveKeepsDraftAndAllowsCorrection() = runTest {
        val controller = FinancialDialogController<String>(backgroundScope) {
            MutationResult.Rejected(FinancialError.Forbidden)
        }
        controller.open("original")
        controller.submit()
        runCurrent()
        assertEquals(FinancialError.Forbidden, controller.dialogState.value?.rejection)
        assertFalse(checkNotNull(controller.dialogState.value).submitting)
        controller.edit { "corrected" }
        assertEquals("corrected", controller.dialogState.value?.draft)
        assertNull(controller.dialogState.value?.rejection)
    }

    @Test
    fun gatewayFailureBecomesRecoverableDialogError() = runTest {
        val controller = FinancialDialogController<String>(backgroundScope) {
            error("Connection lost")
        }
        controller.open("draft")
        controller.submit()
        runCurrent()
        assertEquals(FinancialError.StorageUnavailable, controller.dialogState.value?.rejection)
        assertFalse(checkNotNull(controller.dialogState.value).submitting)
        controller.dismiss()
        assertNull(controller.dialogState.value)
    }
}
