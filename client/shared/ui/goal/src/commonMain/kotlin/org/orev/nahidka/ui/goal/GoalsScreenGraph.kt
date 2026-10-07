package org.orev.nahidka.ui.goal

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.core.common.IdentifierGeneratorBindings
import org.orev.nahidka.feature.goals.di.GoalsBindings
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.service.GoalsManager

@DependencyGraph(GoalsSessionScope::class, bindingContainers = [GoalsBindings::class, IdentifierGeneratorBindings::class])
interface GoalsScreenGraph {
    val goalsViewModel: GoalsViewModel
    val goalsManager: GoalsManager
}
