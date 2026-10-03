package org.orev.nahidka.feature.tasks.dto

data class TaskUpdateRequest(
    val taskIdentifier: String,
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null
)
