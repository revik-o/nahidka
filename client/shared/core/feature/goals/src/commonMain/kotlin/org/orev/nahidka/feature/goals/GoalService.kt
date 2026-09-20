package org.orev.nahidka.feature.goals

import org.orev.nahidka.api.Goal
import org.orev.nahidka.api.GoalsApi

class GoalService(private val goalsApi: GoalsApi) {
    suspend fun createGoal(goal: Goal) {
        goalsApi.createGoal(goal)
    }
}
