package org.orev.nahidka.feature.tasks.subscription

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext

class TasksSubscription internal constructor(private val subscriptionJob: Job) {
    val isActive: Boolean
        get() = subscriptionJob.isActive

    fun cancel() = subscriptionJob.cancel()

    suspend fun cancelAndJoin() {
        check(currentCoroutineContext()[Job] !== subscriptionJob) {
            "Use cancel() from a task subscription callback"
        }

        subscriptionJob.cancelAndJoin()
    }
}
