package org.orev.nahidka.ui.models

typealias TaskEntity = org.orev.nahidka.api.Task
typealias GoalEntity = org.orev.nahidka.api.Goal

data class NotificationEntity(
    val identifier: String,
    val title: String,
    val message: String,
    val timestamp: Long
)
