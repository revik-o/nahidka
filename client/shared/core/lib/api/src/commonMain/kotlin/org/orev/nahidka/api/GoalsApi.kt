package org.orev.nahidka.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class GoalsApi {

    fun getGoals(): Deferred<List<Goal>> {
        val deferred = CompletableDeferred<List<Goal>>()
        deferred.complete(emptyList())
        return deferred
    }

    fun createGoal(goal: Goal): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }

    fun updateGoal(goalId: String, title: String): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }
}
