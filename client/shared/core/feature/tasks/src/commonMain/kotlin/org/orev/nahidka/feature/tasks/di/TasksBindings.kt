package org.orev.nahidka.feature.tasks.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.feature.tasks.service.TasksRepository

@BindingContainer
object TasksBindings {

    @Provides
    private fun provideTasksRepository(tasksContext: TasksContext): TasksRepository = tasksContext

    @Provides
    @SingleIn(TasksSessionScope::class)
    private fun provideTasksContext(): TasksContext = TasksContext()
}
