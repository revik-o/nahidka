package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.RandomIdentifierGenerator
import org.orev.nahidka.feature.tasks.di.TasksBindings
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.service.TasksManager

@DependencyGraph(TasksSessionScope::class, bindingContainers = [TasksBindings::class])
interface TasksScreenGraph {
    val tasksViewModel: TasksViewModel
    val tasksManager: TasksManager

    @Provides
    fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
