package org.orev.nahidka.feature.tasks.dto

data class TaskRecord(
    val taskIdentifier: String,
    val title: String,
    val description: String,
    val status: TaskStatus
)
