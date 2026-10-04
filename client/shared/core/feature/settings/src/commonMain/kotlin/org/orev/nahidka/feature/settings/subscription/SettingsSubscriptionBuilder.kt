package org.orev.nahidka.feature.settings.subscription

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.settings.dto.SettingsNotification
import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.dto.SettingsUpdated
import org.orev.nahidka.feature.settings.service.SettingsContext

class SettingsSubscriptionBuilder internal constructor(
    private val settingsContext: SettingsContext,
    private val subscriptionBufferCapacity: Int,
    private val subscriptionCallbacks: SettingsSubscriptionCallbacks = SettingsSubscriptionCallbacks()
) {
    fun onSnapshot(
        callback: suspend (SettingsSnapshot) -> Unit
    ): SettingsSubscriptionBuilder = SettingsSubscriptionBuilder(
        settingsContext,
        subscriptionBufferCapacity,
        subscriptionCallbacks.copy(onSnapshot = callback)
    )

    fun onUpdate(
        callback: suspend (SettingsUpdated) -> Unit
    ): SettingsSubscriptionBuilder = SettingsSubscriptionBuilder(
        settingsContext,
        subscriptionBufferCapacity,
        subscriptionCallbacks.copy(onUpdate = callback)
    )

    fun onFailure(
        callback: suspend (Throwable) -> Unit
    ): SettingsSubscriptionBuilder = SettingsSubscriptionBuilder(
        settingsContext,
        subscriptionBufferCapacity,
        subscriptionCallbacks.copy(onFailure = callback)
    )

    suspend fun startIn(coroutineScope: CoroutineScope): SettingsSubscription {
        currentCoroutineContext().ensureActive()
        val observationJob = requireNotNull(coroutineScope.coroutineContext[Job]) {
            "Settings observation scope must contain a lifecycle Job"
        }
        observationJob.ensureActive()
        val notificationChannel = Channel<SettingsNotification>(subscriptionBufferCapacity)
        val registrationCompleted = CompletableDeferred<Unit>()
        val subscriptionOwnership = SettingsSubscriptionOwnership()
        val subscriptionJob = coroutineScope.launch(subscriptionOwnership) {
            settingsContext.collectNotifications(
                notificationChannel,
                subscriptionCallbacks,
                registrationCompleted
            )
        }
        subscriptionJob.invokeOnCompletion { failure ->
            registrationCompleted.completeExceptionally(
                failure ?: IllegalStateException("Settings subscription ended before registration")
            )
        }
        return try {
            registrationCompleted.await()
            currentCoroutineContext().ensureActive()
            SettingsSubscription(subscriptionJob, subscriptionOwnership)
        } catch (failure: Throwable) {
            subscriptionJob.cancel()
            withContext(NonCancellable) {
                subscriptionJob.join()
                notificationChannel.cancel()
            }
            throw failure
        }
    }
}
