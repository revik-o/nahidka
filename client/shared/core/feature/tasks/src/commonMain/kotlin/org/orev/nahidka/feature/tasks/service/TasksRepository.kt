package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot

interface TasksRepository {

    val tasksState: StateFlow<TasksSnapshot>

    suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult
    suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult
    suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult
}
