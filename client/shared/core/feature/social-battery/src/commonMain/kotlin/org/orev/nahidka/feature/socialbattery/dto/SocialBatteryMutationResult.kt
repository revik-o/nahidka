package org.orev.nahidka.feature.socialbattery.dto

data class SocialBatteryMutationResult(
    val revision: Long,
    val socialBattery: SocialBattery,
    val hasChanged: Boolean
)
