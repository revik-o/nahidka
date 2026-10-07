package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.core.common.IdentifierGeneratorBindings
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class, IdentifierGeneratorBindings::class])
interface TasksScreenGraph {
    val tasksViewModel: TasksViewModel
    val tasksManager: TasksManager
}
