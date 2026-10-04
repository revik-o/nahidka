package org.orev.nahidka.feature.socialbattery.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryInserted
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryNotification
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryUpdated
import org.orev.nahidka.feature.socialbattery.subscription.SocialBatterySubscriptionBuilder
import org.orev.nahidka.feature.socialbattery.subscription.SocialBatterySubscriptionCallbacks
import org.orev.nahidka.feature.socialbattery.subscription.SocialBatterySubscriptionOverflowException

class SocialBatteryContext(initialSocialBattery: SocialBattery? = null) {
    private val stateMutex = Mutex()
    private var socialBattery = initialSocialBattery
    private var revision = 0L
    private val subscribers = mutableSetOf<Channel<SocialBatteryNotification>>()

    fun subscribe(
        subscriptionBufferCapacity: Int = DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY
    ): SocialBatterySubscriptionBuilder {
        require(subscriptionBufferCapacity in 1 until Channel.UNLIMITED) {
            "Subscription buffer capacity must be positive and bounded"
        }
        return SocialBatterySubscriptionBuilder(this, subscriptionBufferCapacity)
    }

    suspend fun currentSnapshot(): SocialBatterySnapshot = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        SocialBatterySnapshot(revision, socialBattery)
    }

    internal suspend fun applyBatteryUpdate(
        requestedSocialBattery: SocialBattery
    ): SocialBatteryMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val previousSocialBattery = socialBattery
        if (previousSocialBattery == requestedSocialBattery) {
            return@withLock SocialBatteryMutationResult(revision, requestedSocialBattery, false)
        }
        check(revision < Long.MAX_VALUE) { "Social battery revision limit reached" }
        val nextRevision = revision + 1
        val notification: SocialBatteryNotification = if (previousSocialBattery == null) {
            SocialBatteryInserted(nextRevision, requestedSocialBattery)
        } else {
            SocialBatteryUpdated(nextRevision, previousSocialBattery, requestedSocialBattery)
        }
        val result = SocialBatteryMutationResult(nextRevision, requestedSocialBattery, true)
        currentCoroutineContext().ensureActive()
        socialBattery = requestedSocialBattery
        revision = nextRevision
        publishNotification(notification)
        result
    }

    internal suspend fun collectNotifications(
        notifications: Channel<SocialBatteryNotification>,
        callbacks: SocialBatterySubscriptionCallbacks,
        registrationCompleted: CompletableDeferred<Unit>
    ) {
        var subscriptionFailure: Throwable? = null
        try {
            stateMutex.withLock {
                currentCoroutineContext().ensureActive()
                check(notifications.trySend(SocialBatterySnapshot(revision, socialBattery)).isSuccess)
                subscribers.add(notifications)
            }
            registrationCompleted.complete(Unit)
            for (notification in notifications) {
                stateMutex.withLock {
                    currentCoroutineContext().ensureActive()
                }
                when (notification) {
                    is SocialBatterySnapshot -> callbacks.onSnapshot(notification)
                    is SocialBatteryUpdated -> callbacks.onUpdate(notification)
                    is SocialBatteryInserted -> callbacks.onInsert(notification)
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

    private fun publishNotification(notification: SocialBatteryNotification) {
        val subscribersIterator = subscribers.iterator()
        while (subscribersIterator.hasNext()) {
            val notifications = subscribersIterator.next()
            val deliveryResult = notifications.trySend(notification)
            if (deliveryResult.isFailure) {
                subscribersIterator.remove()
                if (!deliveryResult.isClosed) {
                    notifications.close(SocialBatterySubscriptionOverflowException(notification.revision))
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_SUBSCRIPTION_BUFFER_CAPACITY = 64
    }
}
