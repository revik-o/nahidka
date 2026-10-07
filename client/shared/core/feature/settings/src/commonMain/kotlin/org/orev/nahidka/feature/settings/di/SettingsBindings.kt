package org.orev.nahidka.feature.settings.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource

@BindingContainer
object SettingsBindings {

    @Provides
    @SingleIn(SettingsSessionScope::class)
    private fun provideSettingsContext(localDataSource: SettingsLocalDataSource): SettingsContext =
        SettingsContext(localDataSource)
}
