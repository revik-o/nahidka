package org.orev.nahidka.feature.tasks.dto

data class TaskCreationRequest(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0
)
