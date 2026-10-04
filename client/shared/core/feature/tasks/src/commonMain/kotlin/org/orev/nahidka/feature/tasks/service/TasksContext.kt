package org.orev.nahidka.feature.tasks.service

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.tasks.dto.*

class TasksContext : TasksRepository {

    private val stateMutex = Mutex()
    private var tasksByIdentifier = persistentMapOf<String, TaskRecord>()
    private val mutableTasksState: MutableStateFlow<TasksSnapshot>

    override val tasksState: StateFlow<TasksSnapshot>

    constructor(initialTasks: List<TaskRecord> = emptyList()) {
        for (task in initialTasks) {
            validateTask(task)
            require(task.identifier !in tasksByIdentifier) { "Duplicate initial task identifier: ${task.identifier}" }
            tasksByIdentifier = tasksByIdentifier.putting(task.identifier, task)
        }

        mutableTasksState = MutableStateFlow(TasksSnapshot(0, tasksByIdentifier))
        tasksState = mutableTasksState.asStateFlow()
    }

    fun currentSnapshot(): TasksSnapshot = tasksState.value

    override suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val insertedTasks = ArrayList<TaskRecord>(requests.size)

        for (request in requests) {
            val task =
                TaskRecord(request.identifier, request.title, request.description, request.status, request.priority)

            validateTask(task)
            require(task.identifier !in nextTasks) {
                "Task already exists: ${task.identifier}"
            }

            nextTasks = nextTasks.putting(task.identifier, task)
            insertedTasks.add(task)
        }

        commit(nextTasks, insertedTasks)
    }

    override suspend fun deleteTasks(taskIdentifiers: List<String>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val deletedTasks = ArrayList<TaskRecord>(taskIdentifiers.size)

        for (identifier in taskIdentifiers) {
            require(identifier.isNotBlank()) { "Task identifier must not be blank" }
            val task = requireNotNull(nextTasks[identifier]) { "Unknown or duplicate task identifier: $identifier" }
            nextTasks = nextTasks.removing(identifier)
            deletedTasks.add(task)
        }

        commit(nextTasks, deletedTasks)
    }

    override suspend fun updateTasks(requests: List<TaskUpdateRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val updatedTasks = ArrayList<TaskRecord>(requests.size)
        val identifiers = HashSet<String>(requests.size)

        for (request in requests) {
            require(identifiers.add(request.identifier)) {
                "Duplicate task identifier: ${request.identifier}"
            }

            val previousTask = requireNotNull(nextTasks[request.identifier]) {
                "Unknown task identifier: ${request.identifier}"
            }

            val currentTask = previousTask.copy(
                title = request.title ?: previousTask.title,
                description = when (val patch = request.descriptionPatch) {
                    NullablePatch.Keep -> previousTask.description
                    NullablePatch.Clear -> ""
                    is NullablePatch.Set -> patch.value
                },
                status = request.status ?: previousTask.status,
                priority = request.priority ?: previousTask.priority
            )

            validateTask(currentTask)

            if (previousTask != currentTask) {
                nextTasks = nextTasks.putting(currentTask.identifier, currentTask)
                updatedTasks.add(currentTask)
            }
        }

        commit(nextTasks, updatedTasks)
    }

    private suspend fun commit(
        nextTasks: kotlinx.collections.immutable.PersistentMap<String, TaskRecord>,
        affectedTasks: List<TaskRecord>
    ): TasksMutationResult {
        val previousSnapshot = mutableTasksState.value

        if (affectedTasks.isEmpty()) {
            return TasksMutationResult(previousSnapshot.revision, affectedTasks)
        }

        val nextRevision = incrementRevision(previousSnapshot.revision)
        val nextSnapshot = TasksSnapshot(nextRevision, nextTasks)
        val result = TasksMutationResult(nextRevision, affectedTasks)

        currentCoroutineContext().ensureActive()
        tasksByIdentifier = nextTasks
        mutableTasksState.value = nextSnapshot

        return result
    }

    private fun validateTask(task: TaskRecord) {
        require(task.identifier.isNotBlank()) { "Task identifier must not be blank" }
        require(task.title.isNotBlank()) { "Task title must not be blank" }
    }
}
