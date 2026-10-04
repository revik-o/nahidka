package org.orev.nahidka.feature.socialbattery.subscription

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext

class SocialBatterySubscription internal constructor(
    private val subscriptionJob: Job,
    private val subscriptionOwnership: SocialBatterySubscriptionOwnership
) {
    val isActive: Boolean
        get() = subscriptionJob.isActive

    fun cancel() = subscriptionJob.cancel()

    suspend fun cancelAndJoin() {
        check(currentCoroutineContext()[SocialBatterySubscriptionOwnership] !== subscriptionOwnership) {
            "Use cancel() from a social battery subscription callback or its child coroutine"
        }
        subscriptionJob.cancelAndJoin()
    }
}
