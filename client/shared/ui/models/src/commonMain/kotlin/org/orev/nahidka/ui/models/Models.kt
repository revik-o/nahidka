package org.orev.nahidka.ui.models

data class TaskEntity(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: Int
)

data class GoalEntity(
    val id: String,
    val title: String,
    val progress: Float,
    val deadline: String
)

data class NotificationEntity(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long
)
