package org.orev.nahidka.feature.settings.subscription

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext

class SettingsSubscription internal constructor(
    private val subscriptionJob: Job,
    private val subscriptionOwnership: SettingsSubscriptionOwnership
) {
    val isActive: Boolean
        get() = subscriptionJob.isActive

    fun cancel() = subscriptionJob.cancel()

    suspend fun cancelAndJoin() {
        check(currentCoroutineContext()[SettingsSubscriptionOwnership] !== subscriptionOwnership) {
            "Use cancel() from a settings subscription callback or its child coroutine"
        }
        subscriptionJob.cancelAndJoin()
    }
}
