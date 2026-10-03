package org.orev.nahidka.feature.tasks.dto

data class TaskChange(
    val previousTask: TaskRecord,
    val currentTask: TaskRecord
)
