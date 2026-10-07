package org.orev.nahidka.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.feature.tasks.service.TasksManager
import org.orev.nahidka.feature.tasks.service.TasksRepository
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.common.mutation.mutationRejectionOf
import org.orev.nahidka.ui.tasks.model.*

private const val TASKS_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L

@Inject
class TasksViewModel(
    private val tasksRepository: TasksRepository,
    private val tasksManager: TasksManager,
    private val identifierGenerator: IdentifierGenerator,
) : ViewModel() {

    private val mutableChangeRejections = MutableSharedFlow<IllegalArgumentException>(extraBufferCapacity = 1)

    val tasksContent: StateFlow<TasksContent> = tasksRepository.tasksState
        .map(TasksContent::of)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TASKS_CONTENT_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = TasksContent.of(tasksRepository.tasksState.value),
        )

    val changeRejections: SharedFlow<IllegalArgumentException> = mutableChangeRejections.asSharedFlow()

    val taskEditor = MutationDialogController<TaskDraft>(viewModelScope, TaskDraft::submittable) { taskDraft ->
        saveTask(taskDraft)
    }

    val taskDeletion = MutationDialogController<TaskItem>(viewModelScope) { taskItem ->
        tasksManager.deleteTasks(listOf(taskItem.task.identifier))
    }

    val ratingLevelsEditor = MutationDialogController<TaskRatingLevelsDraft>(
        viewModelScope,
        TaskRatingLevelsDraft::submittable,
    ) { ratingLevelsDraft ->
        tasksManager.replaceRatingLevels(ratingLevelsDraft.ratingLevels)
    }

    val taskInteractions = TaskInteractions(
        onTaskEdit = ::openTaskEditing,
        onTaskDelete = taskDeletion::open,
        onTaskMove = ::moveTask,
        onTaskRate = ::rateTask,
    )

    fun openTaskCreation() {
        taskEditor.open(TaskDraft.creation())
    }

    fun openTaskEditing(taskItem: TaskItem) {
        taskEditor.open(TaskDraft.editing(taskItem))
    }

    fun openRatingLevelsEditing() {
        val ratingLevels = tasksRepository.tasksState.value.ratingLevels

        ratingLevelsEditor.open(TaskRatingLevelsDraft(ratingLevels.toPersistentList()))
    }

    fun addRatingLevel() {
        ratingLevelsEditor.edit { ratingLevelsDraft ->
            ratingLevelsDraft.adding(TaskRatingLevel(identifierGenerator.next(), ""))
        }
    }

    fun moveTask(taskItem: TaskItem, status: TaskStatus) {
        changeTask(TaskUpdateRequest(taskItem.task.identifier, status = status))
    }

    fun rateTask(taskItem: TaskItem, ratingLevel: TaskRatingLevel?) {
        changeTask(
            TaskUpdateRequest(
                identifier = taskItem.task.identifier,
                ratingIdentifierPatch = nullablePatch(taskItem.task.ratingIdentifier, ratingLevel?.identifier),
            ),
        )
    }

    private fun changeTask(taskUpdateRequest: TaskUpdateRequest) {
        viewModelScope.launch {
            mutationRejectionOf { tasksManager.updateTasks(listOf(taskUpdateRequest)) }
                ?.let(mutableChangeRejections::tryEmit)
        }
    }

    private suspend fun saveTask(taskDraft: TaskDraft): TasksMutationResult {
        val editedTask = taskDraft.editedTask

        return if (editedTask == null) {
            tasksManager.createTasks(listOf(taskDraft.toCreationRequest(identifierGenerator.next())))
        } else {
            tasksManager.updateTasks(listOf(taskDraft.toUpdateRequest(editedTask)))
        }
    }
}
