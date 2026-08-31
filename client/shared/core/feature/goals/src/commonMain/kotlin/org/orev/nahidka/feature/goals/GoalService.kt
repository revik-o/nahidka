package org.orev.nahidka.feature.goals

import kotlinx.coroutines.Deferred
import org.orev.nahidka.api.Goal
import org.orev.nahidka.api.GoalsApi

class GoalService(private val goalsApi: GoalsApi) {
    fun createGoal(goal: Goal): Deferred<Unit> {
        return goalsApi.createGoal(goal)
    }
}
