package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsRepository

@BindingContainer
object GoalsBindings {

    @Provides
    private fun provideGoalsRepository(goalsContext: GoalsContext): GoalsRepository = goalsContext

    @Provides
    @SingleIn(GoalsSessionScope::class)
    private fun provideGoalsContext(): GoalsContext = GoalsContext()
}
