package org.orev.nahidka.api

data class User(
    val id: String,
    val username: String,
    val email: String
)

data class UserProfile(
    val id: String,
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
    val id: String,
    val title: String,
    val description: String
)

data class SocialBattery(
    val level: Int
)

data class Goal(
    val id: String,
    val title: String
)
