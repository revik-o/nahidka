package org.orev.nahidka.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardViewModelTest {
    @Test
    fun completingPromiseUpdatesActiveCountAndCanBeUndone() {
        val model = DashboardViewModel()
        model.handleEvent(DashboardEvent.TogglePromise(1))
        assertEquals(1, model.state.value.promises.count { !it.completed })
        assertTrue(model.state.value.promises.first().completed)
        model.handleEvent(DashboardEvent.TogglePromise(1))
        assertFalse(model.state.value.promises.first().completed)
        assertEquals(2, model.state.value.promises.count { !it.completed })
    }

    @Test
    fun newPromiseCanBeCompletedAndAppearsInActivity() {
        val model = DashboardViewModel()
        model.handleEvent(DashboardEvent.AddEntry("Add Promise", "Bring flowers"))
        val added = model.state.value.promises.last()
        assertEquals("Bring flowers", added.title)
        assertEquals(4, added.id)
        model.handleEvent(DashboardEvent.TogglePromise(added.id))
        assertTrue(model.state.value.promises.last().completed)
        assertTrue(model.state.value.activity.first().contains("Bring flowers"))
    }

    @Test
    fun batteryIsClampedAndBlankEntriesAreIgnored() {
        val model = DashboardViewModel()
        val initial = model.state.value
        model.handleEvent(DashboardEvent.AddEntry("New Note", "   "))
        assertEquals(initial, model.state.value)
        model.handleEvent(DashboardEvent.SetBattery(2f))
        assertEquals(1f, model.state.value.socialBatteryLevel)
        model.handleEvent(DashboardEvent.SetBattery(-1f))
        assertEquals(0f, model.state.value.socialBatteryLevel)
    }

    @Test
    fun noteReplacesLoveNote() {
        val model = DashboardViewModel()
        model.handleEvent(DashboardEvent.AddEntry("New Note", "Thinking of you"))
        assertEquals("Thinking of you", model.state.value.loveNote)
    }
}
