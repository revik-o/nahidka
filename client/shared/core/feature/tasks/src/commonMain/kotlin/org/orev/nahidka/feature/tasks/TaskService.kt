package org.orev.nahidka.feature.tasks

import kotlinx.coroutines.Deferred
import org.orev.nahidka.api.Task
import org.orev.nahidka.api.TasksApi

class TaskService(private val tasksApi: TasksApi) {
    fun createNewTask(task: Task): Deferred<Unit> {
        return tasksApi.createTask(task)
    }
}
