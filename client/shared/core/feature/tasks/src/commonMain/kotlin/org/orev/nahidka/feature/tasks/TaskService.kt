package org.orev.nahidka.feature.tasks

import org.orev.nahidka.api.Task
import org.orev.nahidka.api.TasksApi

class TaskService(private val tasksApi: TasksApi) {
    suspend fun createNewTask(task: Task) {
        tasksApi.createTask(task)
    }
}
