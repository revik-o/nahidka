package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch

data class GoalUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val description: String? = null,
    val picturePatch: NullablePatch<GoalPicture> = NullablePatch.Keep,
    val progressPercentage: Float? = null,
    val deadlinePatch: NullablePatch<LocalDate> = NullablePatch.Keep
)
