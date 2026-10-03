package org.orev.nahidka.ui.dashboard

sealed interface DashboardEvent {
    data class TogglePromise(val id: Int) : DashboardEvent
    data class SetBattery(val value: Float) : DashboardEvent
    data class AddEntry(val kind: String, val text: String) : DashboardEvent
}
