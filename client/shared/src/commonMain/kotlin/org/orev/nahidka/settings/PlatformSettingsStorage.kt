package org.orev.nahidka.settings

import androidx.compose.runtime.Composable
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource

@Composable
expect fun rememberSettingsLocalDataSource(): SettingsLocalDataSource
