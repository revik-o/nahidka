package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@DependencyGraph(SocialBatterySessionScope::class)
interface SocialBatterySessionGraph {
    val socialBatteryContext: SocialBatteryContext
    val socialBatteryManager: SocialBatteryManager

    companion object {
        @Provides
        @SingleIn(SocialBatterySessionScope::class)
        private fun provideSocialBatteryContext(): SocialBatteryContext = SocialBatteryContext()
    }
}
