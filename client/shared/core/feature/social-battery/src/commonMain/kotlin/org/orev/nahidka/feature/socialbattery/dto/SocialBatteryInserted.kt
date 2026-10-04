package org.orev.nahidka.feature.socialbattery.dto

data class SocialBatteryInserted(
    override val revision: Long,
    val socialBattery: SocialBattery
) : SocialBatteryNotification
