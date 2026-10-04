package org.orev.nahidka.feature.tasks.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val DEFAULT_TASK_RATING_LEVELS: ImmutableList<TaskRatingLevel> = persistentListOf(
    TaskRatingLevel("bad", "😞"),
    TaskRatingLevel("okay", "😐"),
    TaskRatingLevel("good", "🙂"),
    TaskRatingLevel("excellent", "🤩")
)
