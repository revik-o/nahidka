package org.orev.nahidka.ui.tasks.model

import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.feature.tasks.dto.TaskStatus

class TaskInteractions(
    val onTaskEdit: (TaskItem) -> Unit,
    val onTaskDelete: (TaskItem) -> Unit,
    val onTaskMove: (TaskItem, TaskStatus) -> Unit,
    val onTaskRate: (TaskItem, TaskRatingLevel?) -> Unit,
)
