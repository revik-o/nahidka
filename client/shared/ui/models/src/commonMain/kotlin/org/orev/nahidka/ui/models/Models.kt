package org.orev.nahidka.ui.models

data class TaskEntity(
    val id: String,
    val title: String,
    val description: String,
    val status: String, // e.g. "To Do", "In Progress", "Done"
    val priority: Int
)

data class GoalEntity(
    val id: String,
    val title: String,
    val progress: Float, // 0.0 to 1.0
    val deadline: String
)

data class TransactionEntity(
    val id: String,
    val title: String,
    val amount: Double,
    val iconName: String,
    val isSelected: Boolean = false
)

data class NotificationEntity(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long
)
