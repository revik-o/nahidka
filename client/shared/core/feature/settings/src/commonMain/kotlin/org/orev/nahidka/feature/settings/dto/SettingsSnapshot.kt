package org.orev.nahidka.feature.settings.dto

data class SettingsSnapshot(
    val revision: Long,
    val settings: Settings
)
