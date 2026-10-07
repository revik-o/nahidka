package org.orev.nahidka

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GoalsHostTest : ApplicationTest() {

    @Test
    fun goalsEntryUsesDemoDataAndPreservesEditsAcrossNavigation() = runComposeUiTest {
        setApplicationContent()
        openDestination("Goals")
        onNodeWithText("Run a half marathon")
            .assertIsDisplayed()
        onNodeWithText("Run a half marathon")
            .performClick()
        onNodeWithText("Title")
            .performTextReplacement("Run a full marathon")
        onNodeWithText("Save")
            .performClick()
        onNodeWithText("Run a full marathon")
            .assertIsDisplayed()
        openDestination("Dashboard")
        openDestination("Goals")
        onNodeWithText("Run a full marathon")
            .assertIsDisplayed()
    }
}
