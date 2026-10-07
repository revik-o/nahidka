package org.orev.nahidka.feature.goals.service

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.applyTo
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.goals.dto.*

class GoalsContext : GoalsRepository {

    private val stateMutex = Mutex()
    private var goalsByIdentifier = persistentMapOf<String, GoalRecord>()
    private val mutableGoalsState: MutableStateFlow<GoalsSnapshot>
    override val goalsState: StateFlow<GoalsSnapshot>

    constructor(initialGoals: List<GoalRecord> = emptyList()) {
        for (goal in initialGoals) {
            validateGoal(goal)
            require(goal.identifier !in goalsByIdentifier) { "Duplicate initial goal identifier: ${goal.identifier}" }
            goalsByIdentifier = goalsByIdentifier.putting(goal.identifier, goal)
        }
        mutableGoalsState = MutableStateFlow(GoalsSnapshot(0, goalsByIdentifier))
        goalsState = mutableGoalsState.asStateFlow()
    }

    fun currentSnapshot(): GoalsSnapshot = goalsState.value

    override suspend fun createGoal(request: GoalCreationRequest): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val goal = GoalRecord(
            request.identifier,
            request.title,
            request.description,
            request.picture,
            request.progressPercentage,
            request.deadlineDate
        )

        validateGoal(goal)
        require(goal.identifier !in goalsByIdentifier) { "Goal already exists: ${goal.identifier}" }
        commit(goalsByIdentifier.putting(goal.identifier, goal), goal)
    }

    override suspend fun deleteGoal(identifier: String): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val goal = requireNotNull(goalsByIdentifier[identifier]) {
            "Unknown goal identifier: $identifier"
        }

        commit(goalsByIdentifier.removing(identifier), goal)
    }

    override suspend fun updateGoal(request: GoalUpdateRequest): GoalsMutationResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val previousGoal = requireNotNull(goalsByIdentifier[request.identifier]) {
            "Unknown goal identifier: ${request.identifier}"
        }
        val currentGoal = previousGoal.copy(
            title = request.title ?: previousGoal.title,
            description = request.description ?: previousGoal.description,
            picture = request.picturePatch.applyTo(previousGoal.picture),
            progressPercentage = request.progressPercentage ?: previousGoal.progressPercentage,
            deadlineDate = request.deadlinePatch.applyTo(previousGoal.deadlineDate)
        )

        validateGoal(currentGoal)

        if (previousGoal == currentGoal) {
            return@withLock GoalsMutationResult(
                goalsState.value.revision,
                currentGoal,
                false
            )
        }

        commit(goalsByIdentifier.putting(currentGoal.identifier, currentGoal), currentGoal)
    }

    private suspend fun commit(nextGoals: PersistentMap<String, GoalRecord>, goal: GoalRecord): GoalsMutationResult {
        val nextRevision = incrementRevision(goalsState.value.revision)
        val nextSnapshot = GoalsSnapshot(nextRevision, nextGoals)

        currentCoroutineContext().ensureActive()
        goalsByIdentifier = nextGoals
        mutableGoalsState.value = nextSnapshot

        return GoalsMutationResult(nextRevision, goal, true)
    }

    private fun validateGoal(goal: GoalRecord) {
        require(goal.identifier.isNotBlank()) { "Goal identifier must not be blank" }
        require(goal.title.isNotBlank()) { "Goal title must not be blank" }
        require(goal.progressPercentage in GoalRecord.PROGRESS_PERCENTAGE_RANGE) { "Goal progress must be between 0 and 100" }

        when (val picture = goal.picture) {
            is GoalPicture.Emoji -> require(picture.symbol.isNotBlank()) { "Goal emoji must not be blank" }
            is GoalPicture.Photo -> require(picture.content.isNotEmpty()) { "Goal photo must not be empty" }
            null -> Unit
        }
    }
}
