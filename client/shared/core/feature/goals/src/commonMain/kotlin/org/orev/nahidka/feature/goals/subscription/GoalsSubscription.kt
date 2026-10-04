package org.orev.nahidka.feature.goals.subscription

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext

class GoalsSubscription internal constructor(
    private val subscriptionJob: Job,
    private val subscriptionOwnership: GoalsSubscriptionOwnership
) {
    val isActive: Boolean
        get() = subscriptionJob.isActive

    fun cancel() = subscriptionJob.cancel()

    suspend fun cancelAndJoin() {
        check(currentCoroutineContext()[GoalsSubscriptionOwnership] !== subscriptionOwnership) {
            "Use cancel() from a goals subscription callback or its child coroutine"
        }
        subscriptionJob.cancelAndJoin()
    }
}
