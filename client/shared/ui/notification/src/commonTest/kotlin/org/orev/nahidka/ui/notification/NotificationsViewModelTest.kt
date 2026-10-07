package org.orev.nahidka.ui.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.feature.goals.service.GoalsContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.tasks.dto.TaskCreationRequest
import org.orev.nahidka.feature.tasks.service.TasksContext
import org.orev.nahidka.ui.notification.reminder.ReminderKind
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val tasksContext = TasksContext()

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun dismissedReminderStaysHiddenWhileNewRemindersAppear() = runTest {
        val notificationsViewModel = NotificationsViewModel(
            tasksRepository = tasksContext,
            goalsRepository = GoalsContext(),
            socialBatteryContext = SocialBatteryContext(),
            applicationClock = ApplicationClock { Instant.parse("2026-10-07T12:00:00Z") },
            timeZone = TimeZone.UTC,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            notificationsViewModel.reminders.collect {}
        }

        notificationsViewModel.dismiss(notificationsViewModel.reminders.value.single())
        tasksContext.createTasks(listOf(TaskCreationRequest("overdue", "Overdue", dueDate = LocalDate(2026, 10, 1))))

        assertEquals(
            listOf(ReminderKind.TASK_OVERDUE),
            notificationsViewModel.reminders.value.map { reminder -> reminder.kind },
        )
    }
}
