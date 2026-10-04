package org.orev.nahidka.feature.settings.dto

data class SettingsSnapshot(
    override val revision: Long,
    val settings: Settings
) : SettingsNotification
