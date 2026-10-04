package org.orev.nahidka.feature.goals.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult

@SingleIn(GoalsSessionScope::class)
class GoalsManager @Inject constructor(private val goalsContext: GoalsContext) {
    suspend fun createGoal(goalCreationRequest: GoalCreationRequest): GoalsMutationResult =
        goalsContext.insertGoal(goalCreationRequest)

    suspend fun deleteGoal(goalIdentifier: String): GoalsMutationResult =
        goalsContext.removeGoal(goalIdentifier)

    suspend fun updateGoal(goalUpdateRequest: GoalUpdateRequest): GoalsMutationResult =
        goalsContext.applyGoalUpdate(goalUpdateRequest)
}
