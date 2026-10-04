package org.orev.nahidka.feature.socialbattery.dto

data class SocialBatteryUpdated(
    override val revision: Long,
    val previousSocialBattery: SocialBattery,
    val currentSocialBattery: SocialBattery
) : SocialBatteryNotification
