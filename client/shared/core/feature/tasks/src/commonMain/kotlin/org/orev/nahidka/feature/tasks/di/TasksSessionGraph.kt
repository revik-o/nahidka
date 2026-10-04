package org.orev.nahidka.feature.tasks.di

import dev.zacsweers.metro.DependencyGraph
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class])
interface TasksSessionGraph {
    val tasksContext: TasksContext
    val tasksManager: TasksManager
}
