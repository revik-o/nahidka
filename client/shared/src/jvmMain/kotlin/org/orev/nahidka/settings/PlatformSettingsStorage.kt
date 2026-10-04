package org.orev.nahidka.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.settings.service.KeyValueSettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import java.util.prefs.Preferences

@Composable
actual fun rememberSettingsLocalDataSource(): SettingsLocalDataSource = remember {
    preferencesSettingsLocalDataSource(Preferences.userRoot().node("org/orev/nahidka"))
}

internal fun preferencesSettingsLocalDataSource(preferences: Preferences): SettingsLocalDataSource =
    KeyValueSettingsLocalDataSource(
        readValue = { preferences.get("settings", null) },
        writeValue = { value -> withContext(Dispatchers.IO) {
            preferences.put("settings", value)
            preferences.flush()
        } },
    )
