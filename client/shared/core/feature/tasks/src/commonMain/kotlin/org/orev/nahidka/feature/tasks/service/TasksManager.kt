package org.orev.nahidka.feature.tasks.service

import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(TasksSessionScope::class)
class TasksManager @Inject constructor(private val tasksContext: TasksContext) {
    suspend fun addNewTasks(
        vararg taskCreationRequests: TaskCreationRequest
    ): TasksMutationResult = tasksContext.insertTasks(taskCreationRequests.toList())

    suspend fun removeTasks(
        vararg taskIdentifiers: String
    ): TasksMutationResult = tasksContext.deleteTasks(taskIdentifiers.toList())

    suspend fun updateTasks(
        vararg taskUpdateRequests: TaskUpdateRequest
    ): TasksMutationResult = tasksContext.applyTaskUpdates(taskUpdateRequests.toList())
}
