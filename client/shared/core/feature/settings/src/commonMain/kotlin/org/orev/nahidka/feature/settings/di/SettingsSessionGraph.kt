package org.orev.nahidka.feature.settings.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.feature.settings.service.SettingsManager

@DependencyGraph(SettingsSessionScope::class)
interface SettingsSessionGraph {

    val settingsContext: SettingsContext
    val settingsManager: SettingsManager

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides localDataSource: SettingsLocalDataSource): SettingsSessionGraph
    }

    @Provides
    @SingleIn(SettingsSessionScope::class)
    fun provideSettingsContext(localDataSource: SettingsLocalDataSource): SettingsContext =
        SettingsContext(localDataSource)
}
