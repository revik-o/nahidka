@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.orev.nahidka.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.js
import org.orev.nahidka.feature.settings.service.*

private fun readSettings(): String? = js("window.localStorage.getItem('nahidka-settings')")
private fun writeSettings(value: String): Unit = js("{ window.localStorage.setItem('nahidka-settings', value); }")

@Composable
actual fun rememberSettingsLocalDataSource(): SettingsLocalDataSource = remember {
    KeyValueSettingsLocalDataSource(::readSettings) { writeSettings(it) }
}
