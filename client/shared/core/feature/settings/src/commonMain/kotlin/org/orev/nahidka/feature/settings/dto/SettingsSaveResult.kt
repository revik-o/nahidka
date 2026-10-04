package org.orev.nahidka.feature.settings.dto

data class SettingsSaveResult(
    val revision: Long,
    val settings: Settings,
    val changed: Boolean
)
