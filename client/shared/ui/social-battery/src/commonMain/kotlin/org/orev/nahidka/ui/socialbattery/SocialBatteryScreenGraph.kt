package org.orev.nahidka.ui.socialbattery

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.socialbattery.di.SocialBatteryBindings
import org.orev.nahidka.feature.socialbattery.di.SocialBatterySessionScope

@DependencyGraph(SocialBatterySessionScope::class, bindingContainers = [SocialBatteryBindings::class])
interface SocialBatteryScreenGraph {
    val socialBatteryViewModel: SocialBatteryViewModel
}
