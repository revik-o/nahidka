package org.orev.nahidka.feature.goals.dto

data class GoalsMutationResult(
    val revision: Long,
    val goal: GoalRecord,
    val changed: Boolean
)
