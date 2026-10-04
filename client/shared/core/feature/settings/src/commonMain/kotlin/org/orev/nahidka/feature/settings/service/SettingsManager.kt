package org.orev.nahidka.feature.settings.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.settings.di.SettingsSessionScope
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsSaveResult

@SingleIn(SettingsSessionScope::class)
class SettingsManager @Inject constructor(private val settingsContext: SettingsContext) {

    suspend fun saveSettings(settings: Settings): SettingsSaveResult =
        settingsContext.applySettingsSave(settings)
}
