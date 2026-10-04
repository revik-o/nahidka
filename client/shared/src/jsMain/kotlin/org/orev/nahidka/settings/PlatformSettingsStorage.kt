package org.orev.nahidka.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.orev.nahidka.feature.settings.service.*

private fun readSettings(): String? = js("window.localStorage.getItem('nahidka-settings')") as String?
private fun writeSettings(value: String) { js("window.localStorage.setItem('nahidka-settings', value)") }

@Composable
actual fun rememberSettingsLocalDataSource(): SettingsLocalDataSource = remember {
    KeyValueSettingsLocalDataSource(::readSettings) { writeSettings(it) }
}
