package org.orev.nahidka.api

class FeaturesApi {
    val tasks = TasksApi()
    val social = SocialApi()
    val goals = GoalsApi()

    suspend fun getDashboard(): Dashboard {
        return Dashboard("Summary of dashboard")
    }
}
