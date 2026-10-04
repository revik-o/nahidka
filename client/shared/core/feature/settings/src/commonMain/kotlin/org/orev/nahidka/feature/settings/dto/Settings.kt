package org.orev.nahidka.feature.settings.dto

data class Settings(
    val theme: SettingsTheme = SettingsTheme.FOLLOW_SYSTEM,
    val language: SettingsLanguage = SettingsLanguage.FOLLOW_SYSTEM
)
