package org.orev.nahidka.feature.settings.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsNotification
import org.orev.nahidka.feature.settings.dto.SettingsSaveResult
import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.dto.SettingsUpdated
import org.orev.nahidka.feature.settings.subscription.SettingsSubscriptionBuilder
import org.orev.nahidka.feature.settings.subscription.SettingsSubscriptionCallbacks
import org.orev.nahidka.feature.settings.subscription.SettingsSubscriptionOverflowException

class SettingsContext(initialSettings: Settings = Settings()) {
    private val stateMutex = Mutex()
    private var settings = initialSettings
    private var revision = 0L
    private val subscriberChannels = mutableSetOf<Channel<SettingsNotification>>()

    fun subscribe(
        subscriptionBufferCapacity: Int = DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY
    ): SettingsSubscriptionBuilder {
        require(subscriptionBufferCapacity in 1 until Channel.UNLIMITED) {
            "Subscription buffer capacity must be positive and bounded"
        }
        return SettingsSubscriptionBuilder(this, subscriptionBufferCapacity)
    }

    suspend fun currentSnapshot(): SettingsSnapshot = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        SettingsSnapshot(revision, settings)
    }

    internal suspend fun applySettingsSave(requestedSettings: Settings): SettingsSaveResult =
        stateMutex.withLock {
            currentCoroutineContext().ensureActive()
            val previousSettings = settings
            if (previousSettings == requestedSettings) {
                return@withLock SettingsSaveResult(revision, requestedSettings, false)
            }
            check(revision < Long.MAX_VALUE) { "Settings revision limit reached" }
            val nextRevision = revision + 1
            val settingsUpdated = SettingsUpdated(nextRevision, previousSettings, requestedSettings)
            val settingsSaveResult = SettingsSaveResult(nextRevision, requestedSettings, true)
            currentCoroutineContext().ensureActive()
            settings = requestedSettings
            revision = nextRevision
            publishNotification(settingsUpdated)
            settingsSaveResult
        }

    internal suspend fun collectNotifications(
        notificationChannel: Channel<SettingsNotification>,
        subscriptionCallbacks: SettingsSubscriptionCallbacks,
        registrationCompleted: CompletableDeferred<Unit>
    ) {
        var subscriptionFailure: Throwable? = null
        try {
            stateMutex.withLock {
                currentCoroutineContext().ensureActive()
                check(notificationChannel.trySend(SettingsSnapshot(revision, settings)).isSuccess)
                subscriberChannels.add(notificationChannel)
            }
            registrationCompleted.complete(Unit)
            for (settingsNotification in notificationChannel) {
                stateMutex.withLock {
                    currentCoroutineContext().ensureActive()
                }
                when (settingsNotification) {
                    is SettingsSnapshot -> subscriptionCallbacks.onSnapshot(settingsNotification)
                    is SettingsUpdated -> subscriptionCallbacks.onUpdate(settingsNotification)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            registrationCompleted.completeExceptionally(failure)
            subscriptionFailure = failure
        } finally {
            withContext(NonCancellable) {
                stateMutex.withLock { subscriberChannels.remove(notificationChannel) }
                notificationChannel.cancel()
            }
        }
        subscriptionFailure?.let { failure ->
            currentCoroutineContext().ensureActive()
            subscriptionCallbacks.onFailure(failure)
        }
    }

    private fun publishNotification(settingsNotification: SettingsNotification) {
        val subscriberChannelsIterator = subscriberChannels.iterator()
        while (subscriberChannelsIterator.hasNext()) {
            val notificationChannel = subscriberChannelsIterator.next()
            val deliveryResult = notificationChannel.trySend(settingsNotification)
            if (deliveryResult.isFailure) {
                subscriberChannelsIterator.remove()
                if (!deliveryResult.isClosed) {
                    notificationChannel.close(
                        SettingsSubscriptionOverflowException(settingsNotification.revision)
                    )
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY = 64
    }
}
