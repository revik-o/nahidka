package org.orev.nahidka.feature.tasks.service

import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.tasks.dto.*

interface TasksRepository {

    val tasksState: StateFlow<TasksSnapshot>

    suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult
    suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult
    suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult
    suspend fun replaceRatingLevels(ratingLevels: List<TaskRatingLevel>): TasksMutationResult
}
