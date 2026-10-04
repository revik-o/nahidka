package org.orev.nahidka.ui.tasks.model

import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskRecord

data class TaskItem(
    val task: TaskRecord,
    val ratingLevel: TaskRatingLevel?,
)
