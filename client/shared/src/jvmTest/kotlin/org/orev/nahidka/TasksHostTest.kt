package org.orev.nahidka

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
class TasksHostTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun tasksEntryUsesDemoDataAndPreservesEditsAcrossNavigation() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("Tasks")
            .performClick()
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
        onNodeWithText("Overview")
            .performClick()
        onNodeWithText("Tasks")
            .performClick()
        onNodeWithText("Plan my next week")
            .assertIsDisplayed()
        onNodeWithText("To Do (3)")
            .assertIsDisplayed()
    }
}
