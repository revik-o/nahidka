package org.orev.nahidka.ui.dashboard

data class DashboardState(
    val socialBatteryLevel: Float = .87f,
    val upcomingTasks: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val promises: List<DashboardPromise> = listOf(
        DashboardPromise(1, "Plan a weekend getaway", "May 30, 2024", "High"),
        DashboardPromise(2, "Morning coffee in bed", "May 26, 2024", "Medium"),
        DashboardPromise(3, "Take a dance class together", "", "Low", true),
    ),
    val activity: List<String> = listOf("Added a new memory", "Completed a promise", "Updated budget", "Added a note", "Charged social battery"),
    val loveNote: String = "Thanks for always being my favorite person to do life with. 💕",
    val financialOverview: FinancialOverviewUi? = null,
    val financialError: String? = null,
)
