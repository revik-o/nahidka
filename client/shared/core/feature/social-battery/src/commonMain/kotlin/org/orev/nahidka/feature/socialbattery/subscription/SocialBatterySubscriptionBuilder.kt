package org.orev.nahidka.feature.socialbattery.subscription

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryInserted
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryNotification
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryUpdated
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext

class SocialBatterySubscriptionBuilder internal constructor(
    private val socialBatteryContext: SocialBatteryContext,
    private val subscriptionBufferCapacity: Int,
    private val callbacks: SocialBatterySubscriptionCallbacks = SocialBatterySubscriptionCallbacks()
) {
    fun onSnapshot(callback: suspend (SocialBatterySnapshot) -> Unit): SocialBatterySubscriptionBuilder =
        SocialBatterySubscriptionBuilder(socialBatteryContext, subscriptionBufferCapacity, callbacks.copy(onSnapshot = callback))

    fun onUpdate(callback: suspend (SocialBatteryUpdated) -> Unit): SocialBatterySubscriptionBuilder =
        SocialBatterySubscriptionBuilder(socialBatteryContext, subscriptionBufferCapacity, callbacks.copy(onUpdate = callback))

    fun onInsert(callback: suspend (SocialBatteryInserted) -> Unit): SocialBatterySubscriptionBuilder =
        SocialBatterySubscriptionBuilder(socialBatteryContext, subscriptionBufferCapacity, callbacks.copy(onInsert = callback))

    fun onFailure(callback: suspend (Throwable) -> Unit): SocialBatterySubscriptionBuilder =
        SocialBatterySubscriptionBuilder(socialBatteryContext, subscriptionBufferCapacity, callbacks.copy(onFailure = callback))

    suspend fun startIn(coroutineScope: CoroutineScope): SocialBatterySubscription {
        currentCoroutineContext().ensureActive()
        val observationJob = requireNotNull(coroutineScope.coroutineContext[Job]) {
            "Social battery observation scope must contain a lifecycle Job"
        }
        observationJob.ensureActive()
        val notifications = Channel<SocialBatteryNotification>(subscriptionBufferCapacity)
        val registrationCompleted = CompletableDeferred<Unit>()
        val subscriptionOwnership = SocialBatterySubscriptionOwnership()
        val subscriptionJob = coroutineScope.launch(subscriptionOwnership) {
            socialBatteryContext.collectNotifications(notifications, callbacks, registrationCompleted)
        }
        subscriptionJob.invokeOnCompletion { failure ->
            registrationCompleted.completeExceptionally(
                failure ?: IllegalStateException("Social battery subscription ended before registration")
            )
        }
        return try {
            registrationCompleted.await()
            currentCoroutineContext().ensureActive()
            SocialBatterySubscription(subscriptionJob, subscriptionOwnership)
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
