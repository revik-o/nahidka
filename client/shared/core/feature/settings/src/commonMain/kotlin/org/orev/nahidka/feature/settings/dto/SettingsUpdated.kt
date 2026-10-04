package org.orev.nahidka.feature.settings.dto

data class SettingsUpdated(
    override val revision: Long,
    val previousSettings: Settings,
    val currentSettings: Settings
) : SettingsNotification
