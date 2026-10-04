package org.orev.nahidka.feature.goals.dto

import kotlin.time.Instant
import org.orev.nahidka.core.common.NullablePatch

data class GoalUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val progressPercentage: Float? = null,
    val deadlinePatch: NullablePatch<Instant> = NullablePatch.Keep
)
