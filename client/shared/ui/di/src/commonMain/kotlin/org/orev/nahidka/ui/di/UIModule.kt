package org.orev.nahidka.ui.di

import org.orev.nahidka.ui.models.TaskEntity

// Demonstration of Metro DI for UI
// You would define scopes like AppScope or ActivityScope in your core modules.

/*
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Inject

@ContributesTo(AppScope::class)
interface UIModule {
    
    @Provides
    fun provideDefaultTask(): TaskEntity {
        return TaskEntity(
            id = "default",
            title = "Default Task",
            description = "Injected task",
            status = "To Do",
            priority = 1
        )
    }
}

// A ViewModel or StateHolder injected with Metro
class TaskViewModel @Inject constructor(
    private val defaultTask: TaskEntity
) {
    fun loadTasks(): List<TaskEntity> {
        return listOf(defaultTask)
    }
}
*/
