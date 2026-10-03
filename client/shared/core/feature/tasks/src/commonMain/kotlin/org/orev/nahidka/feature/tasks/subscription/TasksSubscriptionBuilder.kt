package org.orev.nahidka.feature.tasks.subscription

import org.orev.nahidka.feature.tasks.dto.TasksDeleted
import org.orev.nahidka.feature.tasks.dto.TasksInserted
import org.orev.nahidka.feature.tasks.dto.TasksNotification
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot
import org.orev.nahidka.feature.tasks.dto.TasksUpdated
import org.orev.nahidka.feature.tasks.service.TasksContext

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksSubscriptionBuilder internal constructor(
    private val tasksContext: TasksContext,
    private val subscriptionBufferCapacity: Int,
    private val callbacks: TasksSubscriptionCallbacks = TasksSubscriptionCallbacks()
) {
    fun onSnapshot(callback: suspend (TasksSnapshot) -> Unit): TasksSubscriptionBuilder =
        TasksSubscriptionBuilder(tasksContext, subscriptionBufferCapacity, callbacks.copy(onSnapshot = callback))

    fun onUpdate(callback: suspend (TasksUpdated) -> Unit): TasksSubscriptionBuilder =
        TasksSubscriptionBuilder(tasksContext, subscriptionBufferCapacity, callbacks.copy(onUpdate = callback))

    fun onDelete(callback: suspend (TasksDeleted) -> Unit): TasksSubscriptionBuilder =
        TasksSubscriptionBuilder(tasksContext, subscriptionBufferCapacity, callbacks.copy(onDelete = callback))

    fun onInsert(callback: suspend (TasksInserted) -> Unit): TasksSubscriptionBuilder =
        TasksSubscriptionBuilder(tasksContext, subscriptionBufferCapacity, callbacks.copy(onInsert = callback))

    fun onFailure(callback: suspend (Throwable) -> Unit): TasksSubscriptionBuilder =
        TasksSubscriptionBuilder(tasksContext, subscriptionBufferCapacity, callbacks.copy(onFailure = callback))

    suspend fun startIn(coroutineScope: CoroutineScope): TasksSubscription {
        currentCoroutineContext().ensureActive()

        val notifications = Channel<TasksNotification>(subscriptionBufferCapacity)
        val registrationCompleted = CompletableDeferred<Unit>()

        val subscriptionJob = coroutineScope.launch {
            tasksContext.collectNotifications(notifications, callbacks, registrationCompleted)
        }

        subscriptionJob.invokeOnCompletion { failure ->
            registrationCompleted.completeExceptionally(
                failure ?: IllegalStateException("Task subscription ended before registration")
            )
        }

        return try {
            registrationCompleted.await()
            currentCoroutineContext().ensureActive()
            TasksSubscription(subscriptionJob)
        } catch (failure: Throwable) {
            subscriptionJob.cancel()

            withContext(NonCancellable) {
                subscriptionJob.join()
                notifications.cancel()
            }

            throw failure
        }
    }
}
