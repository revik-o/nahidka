package org.orev.nahidka.ui.tasks.model

import kotlinx.collections.immutable.PersistentList
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel

data class TaskRatingLevelsDraft(val ratingLevels: PersistentList<TaskRatingLevel>) {

    val submittable: Boolean
        get() = ratingLevels.all { ratingLevel -> ratingLevel.reaction.isNotBlank() }

    fun adding(ratingLevel: TaskRatingLevel): TaskRatingLevelsDraft =
        copy(ratingLevels = ratingLevels.adding(ratingLevel))

    fun replacingReaction(ratingLevelIndex: Int, reaction: String): TaskRatingLevelsDraft =
        copy(
            ratingLevels = ratingLevels.replacingAt(
                ratingLevelIndex,
                ratingLevels[ratingLevelIndex].copy(reaction = reaction),
            ),
        )

    fun moving(ratingLevelIndex: Int, targetIndex: Int): TaskRatingLevelsDraft =
        copy(
            ratingLevels = ratingLevels
                .removingAt(ratingLevelIndex)
                .addingAt(targetIndex, ratingLevels[ratingLevelIndex]),
        )

    fun removing(ratingLevelIndex: Int): TaskRatingLevelsDraft =
        copy(ratingLevels = ratingLevels.removingAt(ratingLevelIndex))
}
