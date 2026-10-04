package org.orev.nahidka.feature.goals.service

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.orev.nahidka.feature.goals.di.GoalsSessionScope
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult

@SingleIn(GoalsSessionScope::class)
class GoalsManager @Inject constructor(private val goalsRepository: GoalsRepository) {

    suspend fun createGoal(goalCreationRequest: GoalCreationRequest): GoalsMutationResult =
        goalsRepository.createGoal(goalCreationRequest)

    suspend fun deleteGoal(identifier: String): GoalsMutationResult =
        goalsRepository.deleteGoal(identifier)

    suspend fun updateGoal(goalUpdateRequest: GoalUpdateRequest): GoalsMutationResult =
        goalsRepository.updateGoal(goalUpdateRequest)
}
