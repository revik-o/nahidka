package org.orev.nahidka.feature.goals.subscription

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.goals.dto.GoalsDeleted
import org.orev.nahidka.feature.goals.dto.GoalsInserted
import org.orev.nahidka.feature.goals.dto.GoalsNotification
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.dto.GoalsUpdated
import org.orev.nahidka.feature.goals.service.GoalsContext

class GoalsSubscriptionBuilder internal constructor(
    private val goalsContext: GoalsContext,
    private val subscriptionBufferCapacity: Int,
    private val callbacks: GoalsSubscriptionCallbacks = GoalsSubscriptionCallbacks()
) {
    fun onSnapshot(callback: suspend (GoalsSnapshot) -> Unit): GoalsSubscriptionBuilder =
        GoalsSubscriptionBuilder(goalsContext, subscriptionBufferCapacity, callbacks.copy(onSnapshot = callback))

    fun onUpdate(callback: suspend (GoalsUpdated) -> Unit): GoalsSubscriptionBuilder =
        GoalsSubscriptionBuilder(goalsContext, subscriptionBufferCapacity, callbacks.copy(onUpdate = callback))

    fun onInsert(callback: suspend (GoalsInserted) -> Unit): GoalsSubscriptionBuilder =
        GoalsSubscriptionBuilder(goalsContext, subscriptionBufferCapacity, callbacks.copy(onInsert = callback))

    fun onDelete(callback: suspend (GoalsDeleted) -> Unit): GoalsSubscriptionBuilder =
        GoalsSubscriptionBuilder(goalsContext, subscriptionBufferCapacity, callbacks.copy(onDelete = callback))

    fun onFailure(callback: suspend (Throwable) -> Unit): GoalsSubscriptionBuilder =
        GoalsSubscriptionBuilder(goalsContext, subscriptionBufferCapacity, callbacks.copy(onFailure = callback))

    suspend fun startIn(coroutineScope: CoroutineScope): GoalsSubscription {
        currentCoroutineContext().ensureActive()
        val observationJob = requireNotNull(coroutineScope.coroutineContext[Job]) {
            "Goals observation scope must contain a lifecycle Job"
        }
        observationJob.ensureActive()
        val notifications = Channel<GoalsNotification>(subscriptionBufferCapacity)
        val registrationCompleted = CompletableDeferred<Unit>()
        val subscriptionOwnership = GoalsSubscriptionOwnership()
        val subscriptionJob = coroutineScope.launch(subscriptionOwnership) {
            goalsContext.collectNotifications(notifications, callbacks, registrationCompleted)
        }
        subscriptionJob.invokeOnCompletion { failure ->
            registrationCompleted.completeExceptionally(
                failure ?: IllegalStateException("Goals subscription ended before registration")
            )
        }
        return try {
            registrationCompleted.await()
            currentCoroutineContext().ensureActive()
            GoalsSubscription(subscriptionJob, subscriptionOwnership)
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
