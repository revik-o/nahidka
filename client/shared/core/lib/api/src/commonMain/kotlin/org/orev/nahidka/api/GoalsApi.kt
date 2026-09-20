package org.orev.nahidka.api

class GoalsApi {

    suspend fun getGoals(): List<Goal> {
        return emptyList()
    }

    suspend fun createGoal(goal: Goal) {
    }

    suspend fun updateGoal(goalId: String, title: String) {
    }
}
