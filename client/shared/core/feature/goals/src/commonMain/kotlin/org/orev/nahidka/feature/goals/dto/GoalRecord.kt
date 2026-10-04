package org.orev.nahidka.feature.goals.dto

import kotlin.time.Instant

data class GoalRecord(
    val identifier: String,
    val title: String,
    val progressPercentage: Float = 0f,
    val deadlineInstant: Instant? = null,
)
