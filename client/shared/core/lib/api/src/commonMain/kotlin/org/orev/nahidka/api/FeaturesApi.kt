package org.orev.nahidka.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class FeaturesApi {
    val tasks = TasksApi()
    val social = SocialApi()
    val goals = GoalsApi()

    fun getDashboard(): Deferred<Dashboard> {
        val deferred = CompletableDeferred<Dashboard>()
        deferred.complete(Dashboard("Summary of dashboard"))
        return deferred
    }
}
