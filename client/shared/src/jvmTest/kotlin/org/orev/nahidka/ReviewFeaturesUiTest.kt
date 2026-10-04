package org.orev.nahidka

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.orev.nahidka.feature.settings.service.KeyValueSettingsLocalDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReviewFeaturesUiTest {
    @Test fun coreFeaturesUpdateScreensAndSettingsSurviveAppRecreation() = runDesktopComposeUiTest(width = 1100, height = 850) {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        var storedSettings: String? = null
        val settingsStorage = KeyValueSettingsLocalDataSource({ storedSettings }) { storedSettings = it }
        val appGeneration = mutableStateOf(0)
        setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                key(appGeneration.value) { App(settingsStorage = settingsStorage) }
            }
        }
        onNodeWithText("tasks", substring = false).performClick()
        onNode(hasSetTextAction()).performTextInput("Review task")
        onNodeWithText("Add", substring = false).performClick()
        onNodeWithText("Review task").assertIsDisplayed().performClick()
        onNodeWithText("in progress").assertIsDisplayed()
        onNodeWithText("goals", substring = false).performClick()
        onNode(hasSetTextAction()).performTextInput("Review goal")
        onNodeWithText("Add", substring = false).performClick()
        onNodeWithText("Review goal").assertIsDisplayed().performClick()
        onNodeWithText("10%").assertIsDisplayed()
        onNodeWithText("social battery", substring = false).performClick()
        onNodeWithText("Choose your current social battery level.").assertIsDisplayed()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }
        onNodeWithText("80%").assertIsDisplayed()
        onNodeWithTag("open-settings").performClick()
        onNodeWithText("light", substring = false).performClick().assertIsSelected()
        onNodeWithText("ukrainian", substring = false).performClick().assertIsSelected()
        runOnIdle {
            assertEquals("LIGHT|UKRAINIAN", storedSettings)
            appGeneration.value++
        }
        onNodeWithTag("open-settings").performClick()
        onNodeWithText("light", substring = false).assertIsSelected()
        onNodeWithText("ukrainian", substring = false).assertIsSelected()
        runOnIdle { owner.viewModelStore.clear() }
    }
}
