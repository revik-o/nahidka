package org.orev.nahidka.feature.socialbattery.dto

data class SocialBatterySnapshot(
    override val revision: Long,
    val socialBattery: SocialBattery?
) : SocialBatteryNotification
