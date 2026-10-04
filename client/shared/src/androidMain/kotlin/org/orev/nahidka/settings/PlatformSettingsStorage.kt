package org.orev.nahidka.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.settings.service.*

@Composable
actual fun rememberSettingsLocalDataSource(): SettingsLocalDataSource {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        val preferences = applicationContext.getSharedPreferences("nahidka-settings", Context.MODE_PRIVATE)
        KeyValueSettingsLocalDataSource(
            readValue = { preferences.getString("settings", null) },
            writeValue = { value -> withContext(Dispatchers.IO) {
                check(preferences.edit().putString("settings", value).commit()) { "Could not save settings" }
            } },
        )
    }
}
