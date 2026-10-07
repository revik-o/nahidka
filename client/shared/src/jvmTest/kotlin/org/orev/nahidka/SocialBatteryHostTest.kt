package org.orev.nahidka

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class SocialBatteryHostTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun socialBatteryEntryPreservesLevelAcrossNavigation() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("Social battery")
            .performClick()
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        onNodeWithContentDescription("Social Battery")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.8f) }
        onNodeWithText("Overview")
            .performClick()
        onNodeWithText("Social battery")
            .performClick()
        onNodeWithContentDescription("Social Battery")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80%"))
        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
    }
}
