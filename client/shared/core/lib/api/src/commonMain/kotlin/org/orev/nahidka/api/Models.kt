package org.orev.nahidka.api

data class User(
    val identifier: String,
    val username: String,
    val email: String
)

data class UserProfile(
    val identifier: String,
    val bio: String
)

data class ApplicationOptions(
    val theme: String,
    val language: String
)

data class Dashboard(
    val summary: String
)

data class Task(
    val identifier: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TO_DO,
    val priority: Int = 0
)

data class SocialBattery(
    val level: Int
)

data class Goal(
    val identifier: String,
    val title: String,
    val progressPercentage: Float = 0f,
    val deadlineInstant: kotlin.time.Instant? = null
)
