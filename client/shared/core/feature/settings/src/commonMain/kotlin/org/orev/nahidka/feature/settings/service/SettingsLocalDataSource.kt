package org.orev.nahidka.feature.settings.service

import org.orev.nahidka.feature.settings.dto.Settings

interface SettingsLocalDataSource {

    fun loadSettings(): Settings
    suspend fun saveSettings(settings: Settings)
}
