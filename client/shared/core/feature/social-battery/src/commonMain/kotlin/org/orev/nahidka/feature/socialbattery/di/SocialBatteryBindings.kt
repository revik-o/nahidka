package org.orev.nahidka.feature.socialbattery.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext

@BindingContainer
object SocialBatteryBindings {

    @Provides
    @SingleIn(SocialBatterySessionScope::class)
    private fun provideSocialBatteryContext(): SocialBatteryContext = SocialBatteryContext()
}
