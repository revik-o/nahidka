package org.orev.nahidka.feature.goals.dto

data class GoalsUpdated(
    override val revision: Long,
    val previousGoal: GoalRecord,
    val currentGoal: GoalRecord
) : GoalsNotification
