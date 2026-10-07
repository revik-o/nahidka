package org.orev.nahidka.feature.goals.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.goals.service.GoalsManager

@DependencyGraph(GoalsSessionScope::class, bindingContainers = [GoalsBindings::class])
interface GoalsSessionGraph {
    val goalsContext: GoalsContext
    val goalsManager: GoalsManager
}
