package org.orev.nahidka

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TasksHostTest : ApplicationTest() {

    @Test
    fun tasksEntryUsesDemoDataAndPreservesEditsAcrossNavigation() = runComposeUiTest {
        setApplicationContent()
        openDestination("Tasks")
        onNodeWithText("Plan the week")
            .assertIsDisplayed()
        onNodeWithText("Plan the week")
            .performClick()
        onNodeWithText("Title")
            .performTextReplacement("Plan my next week")
        onNodeWithText("Save")
            .performClick()
        onNodeWithText("Plan my next week")
            .assertIsDisplayed()
        openDestination("Dashboard")
        onNodeWithText("Plan my next week")
            .assertIsDisplayed()
        openDestination("Tasks")
        onNodeWithText("Plan my next week")
            .assertIsDisplayed()
        onNodeWithText("To Do (3)")
            .assertIsDisplayed()
    }
}
