package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class])
interface SocialBatterySessionGraph {
    val socialBatteryContext: SocialBatteryContext
    val socialBatteryManager: SocialBatteryManager
}
