package org.orev.nahidka.feature.goals.dto

import kotlin.time.Instant
import org.orev.nahidka.core.common.NullablePatch

data class GoalCreationRequest(
    val identifier: String,
    val title: String,
    val progressPercentage: Float = 0f,
    val deadlineInstant: Instant? = null
)
