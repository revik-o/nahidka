package org.orev.nahidka.feature.goals.dto

class GoalsSnapshot(
    override val revision: Long,
    goals: List<GoalRecord>
) : GoalsNotification {
    private val goalRecords = goals.toMutableList()

    val goals: List<GoalRecord>
        get() = goalRecords.toMutableList()
}
