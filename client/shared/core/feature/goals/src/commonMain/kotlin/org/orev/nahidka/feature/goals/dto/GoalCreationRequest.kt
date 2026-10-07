package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate

data class GoalCreationRequest(
    val identifier: String,
    val title: String,
    val description: String = "",
    val picture: GoalPicture? = null,
    val progressPercentage: Float = 0f,
    val deadlineDate: LocalDate? = null
)
