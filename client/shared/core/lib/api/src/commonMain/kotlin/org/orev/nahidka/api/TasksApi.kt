package org.orev.nahidka.api

class TasksApi {

    suspend fun getTasks(): List<Task> {
        return emptyList()
    }

    suspend fun createTask(task: Task) {
    }

    suspend fun updateTaskField(identifier: String, fieldKey: String, newValue: String) {
    }
}
