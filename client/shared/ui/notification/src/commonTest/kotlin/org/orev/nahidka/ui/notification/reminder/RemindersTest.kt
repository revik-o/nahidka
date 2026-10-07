package org.orev.nahidka.ui.notification.reminder

import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.tasks.dto.TaskRecord
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals

private val TODAY = LocalDate(2026, 10, 7)
private val CHARGED_SOCIAL_BATTERY = SocialBatterySnapshot(1, SocialBattery(50))

class RemindersTest {

    @Test
    fun unfinishedDatedTasksBecomeRemindersOrderedByUrgency() {
        val reminders = remindersOf(
            tasks = listOf(
                TaskRecord("later", "Later", dueDate = LocalDate(2026, 10, 10)),
                TaskRecord("soon", "Soon", dueDate = LocalDate(2026, 10, 9)),
                TaskRecord("today", "Today", status = TaskStatus.IN_PROGRESS, dueDate = TODAY),
                TaskRecord("overdue", "Overdue", dueDate = LocalDate(2026, 10, 1)),
                TaskRecord("done", "Done", status = TaskStatus.DONE, dueDate = LocalDate(2026, 10, 1)),
                TaskRecord("undated", "Undated"),
            ),
            goals = emptyList(),
            socialBatterySnapshot = CHARGED_SOCIAL_BATTERY,
            today = TODAY,
        )

        assertEquals(
            listOf(
                ReminderKind.TASK_OVERDUE to "overdue",
                ReminderKind.TASK_DUE_TODAY to "today",
                ReminderKind.TASK_DUE_SOON to "soon",
            ),
            reminders.map { reminder -> reminder.kind to reminder.subjectIdentifier },
        )
    }

    @Test
    fun unfinishedGoalsRemindAboutPassedAndNextWeekDeadlines() {
        val reminders = remindersOf(
            tasks = emptyList(),
            goals = listOf(
                GoalRecord("next-month", "Next month", deadlineDate = LocalDate(2026, 11, 7)),
                GoalRecord("next-week", "Next week", deadlineDate = LocalDate(2026, 10, 14)),
                GoalRecord("passed", "Passed", deadlineDate = LocalDate(2026, 10, 6)),
                GoalRecord("completed", "Completed", progressPercentage = 100f, deadlineDate = LocalDate(2026, 10, 6)),
            ),
            socialBatterySnapshot = CHARGED_SOCIAL_BATTERY,
            today = TODAY,
        )

        assertEquals(
            listOf(
                ReminderKind.GOAL_DEADLINE_PASSED to "passed",
                ReminderKind.GOAL_DEADLINE_SOON to "next-week",
            ),
            reminders.map { reminder -> reminder.kind to reminder.subjectIdentifier },
        )
    }

    @Test
    fun unsetSocialBatteryAsksForLevel() {
        val reminders = remindersOf(
            tasks = emptyList(),
            goals = emptyList(),
            socialBatterySnapshot = SocialBatterySnapshot(0, null),
            today = TODAY,
        )

        assertEquals(listOf(ReminderKind.SOCIAL_BATTERY_UNSET), reminders.map(Reminder::kind))
    }
}
