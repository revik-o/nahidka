package org.orev.nahidka.feature.tasks.service

import org.orev.nahidka.feature.tasks.dto.TaskChange
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.dto.TaskRecord
import org.orev.nahidka.feature.tasks.dto.TaskUpdateRequest
import org.orev.nahidka.feature.tasks.dto.TasksDeleted
import org.orev.nahidka.feature.tasks.dto.TasksInserted
import org.orev.nahidka.feature.tasks.dto.TasksMutationResult
import org.orev.nahidka.feature.tasks.dto.TasksNotification
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot
import org.orev.nahidka.feature.tasks.dto.TasksUpdated
import org.orev.nahidka.feature.tasks.subscription.TasksSubscriptionBuilder
import org.orev.nahidka.feature.tasks.subscription.TasksSubscriptionCallbacks
import org.orev.nahidka.feature.tasks.subscription.TasksSubscriptionOverflowException

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TasksContext(initialTasks: List<TaskRecord> = emptyList()) {
    private val stateMutex = Mutex()
    private var tasksByIdentifier = linkedMapOf<String, TaskRecord>()
    private var revision = 0L
    private val subscribers = mutableSetOf<Channel<TasksNotification>>()

    init {
        initialTasks.forEach { task ->
            validateTask(task)

            require(task.taskIdentifier !in tasksByIdentifier) {
                "Duplicate initial task identifier: ${task.taskIdentifier}"
            }

            tasksByIdentifier[task.taskIdentifier] = task
        }
    }

    fun subscribe(
        subscriptionBufferCapacity: Int = DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY
    ): TasksSubscriptionBuilder {
        require(subscriptionBufferCapacity in 1 until Channel.UNLIMITED) {
            "Subscription buffer capacity must be positive and bounded"
        }

        return TasksSubscriptionBuilder(this, subscriptionBufferCapacity)
    }

    suspend fun currentSnapshot(): TasksSnapshot = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        TasksSnapshot(revision, tasksByIdentifier.values.toList())
    }

    internal suspend fun insertTasks(
        taskCreationRequests: List<TaskCreationRequest>
    ): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        validateIdentifiers(taskCreationRequests.map { request -> request.taskIdentifier })
        val insertedTasks = taskCreationRequests.map { request ->
            val task = TaskRecord(request.taskIdentifier, request.title, request.description, request.status)

            validateTask(task)

            require(task.taskIdentifier !in tasksByIdentifier) {
                "Task already exists: ${task.taskIdentifier}"
            }

            task
        }
        if (insertedTasks.isEmpty()) {
            return@withLock TasksMutationResult(revision, emptyList())
        }
        val nextTasksByIdentifier = LinkedHashMap(tasksByIdentifier)
        insertedTasks.forEach { task -> nextTasksByIdentifier[task.taskIdentifier] = task }
        val nextRevision = nextRevision()
        val notification = TasksInserted(nextRevision, insertedTasks)
        val result = TasksMutationResult(nextRevision, insertedTasks)
        currentCoroutineContext().ensureActive()
        commitTasks(nextTasksByIdentifier, notification)
        result
    }

    internal suspend fun deleteTasks(
        taskIdentifiers: List<String>
    ): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        validateIdentifiers(taskIdentifiers)
        val deletedTasks = taskIdentifiers.map { taskIdentifier ->
            requireNotNull(tasksByIdentifier[taskIdentifier]) {
                "Unknown task identifier: $taskIdentifier"
            }
        }

        if (deletedTasks.isEmpty()) {
            return@withLock TasksMutationResult(revision, emptyList())
        }

        val nextTasksByIdentifier = LinkedHashMap(tasksByIdentifier)

        taskIdentifiers.forEach { taskIdentifier -> nextTasksByIdentifier.remove(taskIdentifier) }

        val nextRevision = nextRevision()
        val notification = TasksDeleted(nextRevision, deletedTasks)
        val result = TasksMutationResult(nextRevision, deletedTasks)

        currentCoroutineContext().ensureActive()
        commitTasks(nextTasksByIdentifier, notification)

        result
    }

    internal suspend fun applyTaskUpdates(
        taskUpdateRequests: List<TaskUpdateRequest>
    ): TasksMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        validateIdentifiers(taskUpdateRequests.map { request -> request.taskIdentifier })
        val taskChanges = taskUpdateRequests.mapNotNull { request ->
            val previousTask = requireNotNull(tasksByIdentifier[request.taskIdentifier]) {
                "Unknown task identifier: ${request.taskIdentifier}"
            }

            val currentTask = previousTask.copy(
                title = request.title ?: previousTask.title,
                description = request.description ?: previousTask.description,
                status = request.status ?: previousTask.status
            )

            validateTask(currentTask)

            if (previousTask == currentTask) {
                null
            } else {
                TaskChange(previousTask, currentTask)
            }
        }

        if (taskChanges.isEmpty()) {
            return@withLock TasksMutationResult(revision, emptyList())
        }

        val nextTasksByIdentifier = LinkedHashMap(tasksByIdentifier)

        taskChanges.forEach { change ->
            nextTasksByIdentifier[change.currentTask.taskIdentifier] = change.currentTask
        }

        val affectedTasks = taskChanges.map { change -> change.currentTask }
        val nextRevision = nextRevision()
        val notification = TasksUpdated(nextRevision, taskChanges)
        val result = TasksMutationResult(nextRevision, affectedTasks)

        currentCoroutineContext().ensureActive()
        commitTasks(nextTasksByIdentifier, notification)

        result
    }

    internal suspend fun collectNotifications(
        notifications: Channel<TasksNotification>,
        callbacks: TasksSubscriptionCallbacks,
        registrationCompleted: CompletableDeferred<Unit>
    ) {
        var subscriptionFailure: Throwable? = null

        try {
            stateMutex.withLock {
                currentCoroutineContext().ensureActive()
                check(notifications.trySend(TasksSnapshot(revision, tasksByIdentifier.values.toList())).isSuccess)
                subscribers.add(notifications)
            }
            registrationCompleted.complete(Unit)
            for (notification in notifications) {
                stateMutex.withLock {
                    currentCoroutineContext().ensureActive()
                }
                when (notification) {
                    is TasksSnapshot -> callbacks.onSnapshot(notification)
                    is TasksUpdated -> callbacks.onUpdate(notification)
                    is TasksDeleted -> callbacks.onDelete(notification)
                    is TasksInserted -> callbacks.onInsert(notification)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            registrationCompleted.completeExceptionally(failure)
            subscriptionFailure = failure
        } finally {
            withContext(NonCancellable) {
                stateMutex.withLock { subscribers.remove(notifications) }
                notifications.cancel()
            }
        }

        subscriptionFailure?.let { failure ->
            currentCoroutineContext().ensureActive()
            callbacks.onFailure(failure)
        }
    }

    private fun commitTasks(
        nextTasksByIdentifier: LinkedHashMap<String, TaskRecord>,
        notification: TasksNotification
    ) {
        tasksByIdentifier = nextTasksByIdentifier
        revision = notification.revision

        val subscribersIterator = subscribers.iterator()

        while (subscribersIterator.hasNext()) {
            val notifications = subscribersIterator.next()
            val deliveryResult = notifications.trySend(notification)

            if (deliveryResult.isFailure) {
                subscribersIterator.remove()

                if (!deliveryResult.isClosed) {
                    notifications.close(TasksSubscriptionOverflowException(notification.revision))
                }
            }
        }
    }

    private fun nextRevision(): Long {
        check(revision < Long.MAX_VALUE) { "Task revision limit reached" }
        return revision + 1
    }

    private fun validateTask(task: TaskRecord) {
        require(task.taskIdentifier.isNotBlank()) { "Task identifier must not be blank" }
        require(task.title.isNotBlank()) { "Task title must not be blank" }
    }

    private fun validateIdentifiers(taskIdentifiers: List<String>) {
        require(taskIdentifiers.all { taskIdentifier -> taskIdentifier.isNotBlank() }) {
            "Task identifier must not be blank"
        }
        require(taskIdentifiers.size == taskIdentifiers.toSet().size) {
            "Task identifiers must be unique within a batch"
        }
    }

    private companion object {
        const val DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY = 64
    }
}
