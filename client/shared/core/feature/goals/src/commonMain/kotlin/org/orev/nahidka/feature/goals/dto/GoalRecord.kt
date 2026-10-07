package org.orev.nahidka.feature.goals.dto

import kotlinx.datetime.LocalDate

data class GoalRecord(
    val identifier: String,
    val title: String,
    val description: String = "",
    val picture: GoalPicture? = null,
    val progressPercentage: Float = 0f,
    val deadlineDate: LocalDate? = null,
) {

    companion object {
        val PROGRESS_PERCENTAGE_RANGE = 0f..100f
    }
}
