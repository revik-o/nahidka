package org.orev.nahidka.feature.goals.dto

data class GoalUpdateRequest(
    val goalIdentifier: String,
    val title: String? = null
)
