package org.orev.nahidka.ui.goal.model

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.nullablePatch
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest

data class GoalDraft(
    val editedGoal: GoalRecord?,
    val title: String,
    val description: String,
    val picture: GoalPicture?,
    val progressPercentage: Float,
    val deadlineDate: LocalDate?,
) {

    val submittable: Boolean
        get() = title.isNotBlank()

    fun toCreationRequest(identifier: String): GoalCreationRequest = GoalCreationRequest(
        identifier = identifier,
        title = title.trim(),
        description = description.trim(),
        picture = picture,
        progressPercentage = progressPercentage,
        deadlineDate = deadlineDate,
    )

    fun toUpdateRequest(editedGoal: GoalRecord): GoalUpdateRequest = GoalUpdateRequest(
        identifier = editedGoal.identifier,
        title = title.trim(),
        description = description.trim(),
        picturePatch = nullablePatch(editedGoal.picture, picture),
        progressPercentage = progressPercentage,
        deadlinePatch = nullablePatch(editedGoal.deadlineDate, deadlineDate),
    )

    companion object {

        fun creation(): GoalDraft = GoalDraft(
            editedGoal = null,
            title = "",
            description = "",
            picture = null,
            progressPercentage = 0f,
            deadlineDate = null,
        )

        fun editing(goal: GoalRecord): GoalDraft = GoalDraft(
            editedGoal = goal,
            title = goal.title,
            description = goal.description,
            picture = goal.picture,
            progressPercentage = goal.progressPercentage,
            deadlineDate = goal.deadlineDate,
        )
    }
}
