package org.orev.nahidka.feature.goals.dto

import org.orev.nahidka.core.common.NullablePatch
import kotlin.time.Instant

data class GoalUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val progressPercentage: Float? = null,
    val deadlinePatch: NullablePatch<Instant> = NullablePatch.Keep
)
