package org.orev.nahidka.feature.tasks.service

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.applyTo
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.tasks.dto.*

class TasksContext : TasksRepository {

    private val stateMutex = Mutex()
    private var tasksByIdentifier = persistentMapOf<String, TaskRecord>()
    private var ratingLevels: PersistentList<TaskRatingLevel>
    private val mutableTasksState: MutableStateFlow<TasksSnapshot>

    override val tasksState: StateFlow<TasksSnapshot>

    constructor(
        initialTasks: List<TaskRecord> = emptyList(),
        initialRatingLevels: List<TaskRatingLevel> = DEFAULT_TASK_RATING_LEVELS
    ) {
        validateRatingLevels(initialRatingLevels)
        ratingLevels = initialRatingLevels.toPersistentList()

        for (task in initialTasks) {
            validateTask(task)
            require(task.identifier !in tasksByIdentifier) { "Duplicate initial task identifier: ${task.identifier}" }
            tasksByIdentifier = tasksByIdentifier.putting(task.identifier, task)
        }

        mutableTasksState = MutableStateFlow(TasksSnapshot(0, tasksByIdentifier, ratingLevels))
        tasksState = mutableTasksState.asStateFlow()
    }

    fun currentSnapshot(): TasksSnapshot = tasksState.value

    override suspend fun createTasks(requests: List<TaskCreationRequest>): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        var nextTasks = tasksByIdentifier
        val insertedTasks = ArrayList<TaskRecord>(requests.size)

        for (request in requests) {
            val task = TaskRecord(
                request.identifier,
                request.title,
                request.description,
                request.status,
                request.priority,
                request.dueDate,
                request.ratingIdentifier
            )

            validateTask(task)
            require(task.identifier !in nextTasks) {
                "Task already exists: ${task.identifier}"
            }

            nextTasks = nextTasks.putting(task.identifier, task)
            insertedTasks.add(task)
        }

        commit(nextTasks, ratingLevels, insertedTasks)
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

        commit(nextTasks, ratingLevels, deletedTasks)
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

            val currentStatus = request.status ?: previousTask.status
            val currentTask = previousTask.copy(
                title = request.title ?: previousTask.title,
                description = request.descriptionPatch
                    .applyTo(previousTask.description)
                    .orEmpty(),
                status = currentStatus,
                priority = request.priority ?: previousTask.priority,
                dueDate = request.dueDatePatch.applyTo(previousTask.dueDate),
                ratingIdentifier = request.ratingIdentifierPatch.applyTo(
                    previousTask.ratingIdentifier.takeIf { currentStatus.acceptsRating }
                )
            )

            validateTask(currentTask)

            if (previousTask != currentTask) {
                nextTasks = nextTasks.putting(currentTask.identifier, currentTask)
                updatedTasks.add(currentTask)
            }
        }

        commit(nextTasks, ratingLevels, updatedTasks)
    }

    override suspend fun replaceRatingLevels(
        ratingLevels: List<TaskRatingLevel>
    ): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        validateRatingLevels(ratingLevels)
        val ratingLevelIdentifiers = ratingLevels.mapTo(HashSet(ratingLevels.size)) { ratingLevel ->
            ratingLevel.identifier
        }

        val unratedTasks = tasksByIdentifier.values
            .filter { task -> task.ratingIdentifier != null && task.ratingIdentifier !in ratingLevelIdentifiers }
            .map { task -> task.copy(ratingIdentifier = null) }

        val nextTasks = unratedTasks.fold(tasksByIdentifier) { nextTasks, task ->
            nextTasks.putting(task.identifier, task)
        }

        commit(nextTasks, ratingLevels.toPersistentList(), unratedTasks)
    }

    private suspend fun commit(
        nextTasks: PersistentMap<String, TaskRecord>,
        nextRatingLevels: PersistentList<TaskRatingLevel>,
        affectedTasks: List<TaskRecord>
    ): TasksMutationResult {
        val previousSnapshot = mutableTasksState.value

        if (affectedTasks.isEmpty() && nextRatingLevels == ratingLevels) {
            return TasksMutationResult(previousSnapshot.revision, affectedTasks)
        }

        val nextRevision = incrementRevision(previousSnapshot.revision)
        val nextSnapshot = TasksSnapshot(nextRevision, nextTasks, nextRatingLevels)
        val result = TasksMutationResult(nextRevision, affectedTasks, changed = true)

        currentCoroutineContext().ensureActive()
        tasksByIdentifier = nextTasks
        ratingLevels = nextRatingLevels
        mutableTasksState.value = nextSnapshot

        return result
    }

    private fun validateTask(task: TaskRecord) {
        require(task.identifier.isNotBlank()) { "Task identifier must not be blank" }
        require(task.title.isNotBlank()) { "Task title must not be blank" }

        task.ratingIdentifier?.let { ratingIdentifier ->
            require(task.status.acceptsRating) { "Task cannot be rated in status ${task.status}: ${task.identifier}" }
            require(ratingLevels.any { ratingLevel -> ratingLevel.identifier == ratingIdentifier }) {
                "Unknown rating level identifier: $ratingIdentifier"
            }
        }
    }

    private fun validateRatingLevels(ratingLevels: List<TaskRatingLevel>) {
        val ratingLevelIdentifiers = HashSet<String>(ratingLevels.size)

        for (ratingLevel in ratingLevels) {
            require(ratingLevel.identifier.isNotBlank()) { "Rating level identifier must not be blank" }
            require(ratingLevel.reaction.isNotBlank()) { "Rating level reaction must not be blank" }
            require(ratingLevelIdentifiers.add(ratingLevel.identifier)) {
                "Duplicate rating level identifier: ${ratingLevel.identifier}"
            }
        }
    }
}
