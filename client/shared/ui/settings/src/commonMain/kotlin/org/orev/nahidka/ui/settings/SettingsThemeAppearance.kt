package org.orev.nahidka.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import org.orev.nahidka.feature.settings.dto.SettingsTheme

@Composable
fun SettingsTheme.isDarkTheme(): Boolean = when (this) {
    SettingsTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    SettingsTheme.LIGHT -> false
    SettingsTheme.DARK -> true
}
