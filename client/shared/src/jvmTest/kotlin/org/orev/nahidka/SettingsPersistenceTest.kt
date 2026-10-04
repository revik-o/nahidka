package org.orev.nahidka

import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.orev.nahidka.feature.settings.dto.*
import org.orev.nahidka.settings.preferencesSettingsLocalDataSource

class SettingsPersistenceTest {
    @Test fun desktopAdapterFlushesPreferencesForANewStorageInstance() = runBlocking {
        val nodePath = "org/orev/nahidka/review-tests/${UUID.randomUUID()}"
        val node = Preferences.userRoot().node(nodePath)
        try {
            val settings = Settings(SettingsTheme.LIGHT, SettingsLanguage.UKRAINIAN)
            preferencesSettingsLocalDataSource(node).saveSettings(settings)
            val restoredNode = Preferences.userRoot().node(nodePath)
            restoredNode.sync()
            assertEquals(settings, preferencesSettingsLocalDataSource(restoredNode).loadSettings())
        } finally {
            node.removeNode()
            Preferences.userRoot().flush()
        }
    }
}
