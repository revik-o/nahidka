package org.orev.nahidka

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StartupUiTest {
    @Test
    fun firstFrameIsReportedOnceAndSessionEditsStillWork() = runDesktopComposeUiTest(width = 800, height = 700) {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        var frames = 0
        setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                App(onFirstFrame = { frames++ })
            }
        }
        waitUntil { frames == 1 }
        onNodeWithText("Good evening, Alex 👋").assertIsDisplayed()
        onNodeWithText("+").performClick()
        onNodeWithText("Add Promise").performClick()
        onNode(hasSetTextAction()).performTextInput("Launch smoke promise")
        onNodeWithText("Add", substring = false).performClick()
        onNodeWithText("Promises", substring = false).performClick()
        onNodeWithText("Launch smoke promise").assertIsDisplayed()
        onNodeWithText("Done").performClick()
        runOnIdle {
            assertEquals(1, frames)
            owner.viewModelStore.clear()
        }
    }

    @Test
    fun lazySectionsRemainReachableAfterChangingLayout() = runDesktopComposeUiTest(width = 1440, height = 900) {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        val width = mutableStateOf(1400.dp)
        var frames = 0
        setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                Box(Modifier.width(width.value).fillMaxHeight()) {
                    App(onFirstFrame = { frames++ })
                }
            }
        }
        waitUntil { frames == 1 }
        onNodeWithText("Dashboard", substring = false).assertIsDisplayed()
        runOnIdle { width.value = 800.dp }
        onNodeWithText("Home", substring = false).assertIsDisplayed()
        onNodeWithText("Dashboard", substring = false).assertDoesNotExist()
        onNode(hasScrollAction()).performScrollToNode(hasText("New Camera"))
        onNodeWithText("New Camera").assertIsDisplayed()
        runOnIdle {
            assertEquals(1, frames)
            owner.viewModelStore.clear()
        }
    }
}
