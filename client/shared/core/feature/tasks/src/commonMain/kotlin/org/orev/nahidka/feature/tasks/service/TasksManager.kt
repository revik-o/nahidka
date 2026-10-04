package org.orev.nahidka.feature.tasks.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.tasks.di.TasksSessionScope
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult

@SingleIn(TasksSessionScope::class)
class TasksManager @Inject constructor(private val tasksRepository: TasksRepository) {

    suspend fun createTasks(
        taskCreationRequests: List<TaskCreationRequest>
    ): TasksMutationResult = tasksRepository.createTasks(taskCreationRequests)

    suspend fun deleteTasks(
        taskIdentifiers: List<String>
    ): TasksMutationResult = tasksRepository.deleteTasks(taskIdentifiers)

    suspend fun updateTasks(
        taskUpdateRequests: List<TaskUpdateRequest>
    ): TasksMutationResult = tasksRepository.updateTasks(taskUpdateRequests)
}
