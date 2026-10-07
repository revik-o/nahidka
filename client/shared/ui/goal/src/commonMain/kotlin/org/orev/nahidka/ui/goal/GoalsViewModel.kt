package org.orev.nahidka.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import org.orev.nahidka.core.common.IdentifierGenerator
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.service.GoalsManager
import org.orev.nahidka.feature.goals.service.GoalsRepository
import org.orev.nahidka.ui.common.dialog.MutationDialogController
import org.orev.nahidka.ui.goal.model.GoalDraft

@Inject
class GoalsViewModel(
    goalsRepository: GoalsRepository,
    private val goalsManager: GoalsManager,
    private val identifierGenerator: IdentifierGenerator,
) : ViewModel() {

    val goalsState: StateFlow<GoalsSnapshot> = goalsRepository.goalsState

    val goalEditor = MutationDialogController<GoalDraft>(viewModelScope, GoalDraft::submittable) { goalDraft ->
        saveGoal(goalDraft)
    }

    val goalDeletion = MutationDialogController<GoalRecord>(viewModelScope) { goal ->
        goalsManager.deleteGoal(goal.identifier)
    }

    fun openGoalCreation() {
        goalEditor.open(GoalDraft.creation())
    }

    fun openGoalEditing(goal: GoalRecord) {
        goalEditor.open(GoalDraft.editing(goal))
    }

    private suspend fun saveGoal(goalDraft: GoalDraft) {
        val editedGoal = goalDraft.editedGoal

        if (editedGoal == null) {
            goalsManager.createGoal(goalDraft.toCreationRequest(identifierGenerator.next()))
        } else {
            goalsManager.updateGoal(goalDraft.toUpdateRequest(editedGoal))
        }
    }
}
