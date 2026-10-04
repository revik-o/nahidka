package org.orev.nahidka.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults
import org.orev.nahidka.feature.settings.service.*

@Composable
actual fun rememberSettingsLocalDataSource(): SettingsLocalDataSource = remember {
    val preferences = NSUserDefaults.standardUserDefaults
    KeyValueSettingsLocalDataSource(
        readValue = { preferences.stringForKey("nahidka-settings") },
        writeValue = { value -> preferences.setObject(value, forKey = "nahidka-settings") },
    )
}
