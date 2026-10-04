package org.orev.nahidka.feature.settings.service

import org.orev.nahidka.feature.settings.dto.*

class KeyValueSettingsLocalDataSource(
    private val readValue: () -> String?,
    private val writeValue: suspend (String) -> Unit,
) : SettingsLocalDataSource {

    override fun loadSettings(): Settings {
        val fields = readValue()?.split('|') ?: return Settings()

        return Settings(
            theme = SettingsTheme.entries.firstOrNull {
                it.name == fields.getOrNull(0)
            } ?: SettingsTheme.FOLLOW_SYSTEM,
            language = SettingsLanguage.entries.firstOrNull {
                it.name == fields.getOrNull(1)
            } ?: SettingsLanguage.ENGLISH,
        )
    }

    override suspend fun saveSettings(settings: Settings) =
        writeValue("${settings.theme.name}|${settings.language.name}")
}
