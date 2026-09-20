package org.orev.nahidka.ui.dashboard

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.orev.nahidka.ui.common.state.StateHolder

data class DashboardPromise(val id: Int, val title: String, val date: String, val priority: String, val completed: Boolean = false)

/** Sample content until the dashboard API exposes the individual feature summaries. */
data class DashboardState(
    val socialBatteryLevel: Float = .87f,
    val upcomingTasks: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val promises: List<DashboardPromise> = listOf(
        DashboardPromise(1, "Plan a weekend getaway", "May 30, 2024", "High"),
        DashboardPromise(2, "Morning coffee in bed", "May 26, 2024", "Medium"),
        DashboardPromise(3, "Take a dance class together", "", "Low", true)
    ),
    val activity: List<String> = listOf("Added a new memory", "Completed a promise", "Updated budget", "Added a note", "Charged social battery"),
    val loveNote: String = "Thanks for always being my favorite person to do life with. 💕"
)

sealed interface DashboardEvent {
    data class TogglePromise(val id: Int) : DashboardEvent
    data class SetBattery(val value: Float) : DashboardEvent
    data class AddEntry(val kind: String, val text: String) : DashboardEvent
}

class DashboardViewModel @Inject constructor() : StateHolder<DashboardState, DashboardEvent>() {
    private val _state = MutableStateFlow(DashboardState())
    override val state: StateFlow<DashboardState> = _state.asStateFlow()

    override fun handleEvent(event: DashboardEvent) {
        _state.update { state ->
            when (event) {
                is DashboardEvent.TogglePromise -> state.copy(promises = state.promises.map { if (it.id == event.id) it.copy(completed = !it.completed) else it })
                is DashboardEvent.SetBattery -> state.copy(socialBatteryLevel = event.value.coerceIn(0f, 1f))
                is DashboardEvent.AddEntry -> if (event.text.isBlank()) state else state.copy(
                    promises = if (event.kind == "Add Promise") state.promises + DashboardPromise((state.promises.maxOfOrNull { it.id } ?: 0) + 1, event.text, "No due date", "Medium") else state.promises,
                    loveNote = if (event.kind == "New Note") event.text else state.loveNote,
                    activity = listOf("${event.kind}: ${event.text}") + state.activity
                )
            }
        }
    }
}
