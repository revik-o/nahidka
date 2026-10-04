package org.orev.nahidka.feature.socialbattery.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult

@Inject
@SingleIn(SocialBatterySessionScope::class)
class SocialBatteryManager(private val socialBatteryContext: SocialBatteryContext) {
    suspend fun updateBattery(socialBattery: SocialBattery): SocialBatteryMutationResult =
        socialBatteryContext.applyBatteryUpdate(socialBattery)
}
