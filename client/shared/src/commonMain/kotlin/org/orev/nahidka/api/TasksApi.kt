package org.orev.nahidka.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class TasksApi {

    fun getTasks(): Deferred<List<Task>> {
        val deferred = CompletableDeferred<List<Task>>()
        deferred.complete(emptyList())
        return deferred
    }

    fun createTask(task: Task): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }

    fun updateTaskField(taskId: String, fieldKey: String, newValue: String): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }
}
