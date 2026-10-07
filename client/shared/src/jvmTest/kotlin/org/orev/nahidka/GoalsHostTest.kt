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
class GoalsHostTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun goalsEntryUsesDemoDataAndPreservesEditsAcrossNavigation() = runComposeUiTest {
        setContent { App() }
        onNodeWithText("Goals")
            .performClick()
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
        onNodeWithText("Overview")
            .performClick()
        onNodeWithText("Goals")
            .performClick()
        onNodeWithText("Run a full marathon")
            .assertIsDisplayed()
    }
}
