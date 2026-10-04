package org.orev.nahidka.feature.tasks.di

import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(TasksSessionScope::class)
interface TasksSessionGraph {
    val tasksContext: TasksContext
    val tasksManager: TasksManager

    companion object {
        @Provides
        private fun provideTasksRepository(context: TasksContext): TasksRepository = context

        @Provides
        @SingleIn(TasksSessionScope::class)
        private fun provideTasksContext(): TasksContext = TasksContext()
    }
}
