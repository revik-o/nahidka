package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager

@DependencyGraph(GoalsSessionScope::class)
interface GoalsSessionGraph {
    val goalsContext: GoalsContext
    val goalsManager: GoalsManager

    companion object {
        @Provides
        @SingleIn(GoalsSessionScope::class)
        private fun provideGoalsContext(): GoalsContext = GoalsContext()
    }
}
