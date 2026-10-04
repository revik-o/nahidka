package org.orev.nahidka.feature.settings.subscription

import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.dto.SettingsUpdated

internal data class SettingsSubscriptionCallbacks(
    val onSnapshot: suspend (SettingsSnapshot) -> Unit = {},
    val onUpdate: suspend (SettingsUpdated) -> Unit = {},
    val onFailure: suspend (Throwable) -> Unit = { failure -> throw failure }
)
