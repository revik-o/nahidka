package org.orev.nahidka

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SocialBatteryHostTest : ApplicationTest() {

    @Test
    fun socialBatteryEntryPreservesLevelAcrossNavigation() = runComposeUiTest {
        setApplicationContent()
        openDestination("Social battery")
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        onNodeWithContentDescription("Social battery")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.8f) }
        openDestination("Dashboard")
        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
        openDestination("Social battery")
        onNodeWithContentDescription("Social battery")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80%"))
        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
    }
}
