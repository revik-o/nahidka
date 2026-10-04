package org.orev.nahidka.feature.goals.dto

data class GoalsInserted(
    override val revision: Long,
    val goal: GoalRecord
) : GoalsNotification
