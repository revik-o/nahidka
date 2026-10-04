package org.orev.nahidka.feature.settings.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsManager

@DependencyGraph(SettingsSessionScope::class)
interface SettingsSessionGraph {
    val settingsContext: SettingsContext
    val settingsManager: SettingsManager

    companion object {
        @Provides
        @SingleIn(SettingsSessionScope::class)
        private fun provideSettingsContext(): SettingsContext = SettingsContext()
    }
}
