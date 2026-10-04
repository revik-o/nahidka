package org.orev.nahidka.feature.socialbattery.subscription

import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryInserted
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryUpdated

internal data class SocialBatterySubscriptionCallbacks(
    val onSnapshot: suspend (SocialBatterySnapshot) -> Unit = {},
    val onUpdate: suspend (SocialBatteryUpdated) -> Unit = {},
    val onInsert: suspend (SocialBatteryInserted) -> Unit = {},
    val onFailure: suspend (Throwable) -> Unit = { failure -> throw failure }
)
