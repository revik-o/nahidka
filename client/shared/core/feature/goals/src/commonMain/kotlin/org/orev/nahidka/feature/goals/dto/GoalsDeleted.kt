package org.orev.nahidka.feature.goals.dto

data class GoalsDeleted(
    override val revision: Long,
    val goal: GoalRecord
) : GoalsNotification
