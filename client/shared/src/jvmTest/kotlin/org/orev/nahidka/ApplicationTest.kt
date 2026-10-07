package org.orev.nahidka

import androidx.compose.ui.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.feature.settings.service.KeyValueSettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
abstract class ApplicationTest {

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun inMemorySettingsLocalDataSource(theme: SettingsTheme = SettingsTheme.DARK): SettingsLocalDataSource {
        var storedSettings: String? = "${theme.name}|${Settings().language.name}"

        return KeyValueSettingsLocalDataSource(
            readValue = { storedSettings },
            writeValue = { settingsValue -> storedSettings = settingsValue },
        )
    }

    protected fun ComposeUiTest.setApplicationContent(theme: SettingsTheme = SettingsTheme.DARK) {
        val settingsLocalDataSource = inMemorySettingsLocalDataSource(theme)

        setContent {
            App(settingsLocalDataSource = settingsLocalDataSource)
        }
    }

    protected fun ComposeUiTest.openDestination(navigationTitle: String) {
        onNode(hasText(navigationTitle) and isSelectable())
            .performClick()
    }
}
