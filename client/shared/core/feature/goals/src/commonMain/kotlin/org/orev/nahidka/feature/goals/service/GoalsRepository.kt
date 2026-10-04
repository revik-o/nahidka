package org.orev.nahidka.feature.goals.service

import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import org.orev.nahidka.feature.goals.dto.GoalsMutationResult
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot

interface GoalsRepository {

    val goalsState: StateFlow<GoalsSnapshot>

    suspend fun createGoal(request: GoalCreationRequest): GoalsMutationResult
    suspend fun deleteGoal(identifier: String): GoalsMutationResult
    suspend fun updateGoal(request: GoalUpdateRequest): GoalsMutationResult
}
