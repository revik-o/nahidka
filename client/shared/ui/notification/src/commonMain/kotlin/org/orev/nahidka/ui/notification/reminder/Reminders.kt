package org.orev.nahidka.ui.notification.reminder

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.tasks.dto.TaskRecord
import org.orev.nahidka.feature.tasks.dto.TaskStatus

private const val TASK_DUE_SOON_DAYS = 2
private const val GOAL_DEADLINE_SOON_DAYS = 7
private const val SOCIAL_BATTERY_SUBJECT_IDENTIFIER = "social-battery"

private val REMINDER_URGENCY_ORDER: Comparator<Reminder> =
    compareBy<Reminder>(Reminder::kind)
        .thenBy(nullsLast()) { reminder -> reminder.date }

internal fun remindersOf(
    tasks: List<TaskRecord>,
    goals: List<GoalRecord>,
    socialBatterySnapshot: SocialBatterySnapshot,
    today: LocalDate,
): List<Reminder> =
    (taskReminders(tasks, today) + goalReminders(goals, today) + socialBatteryReminders(socialBatterySnapshot))
        .sortedWith(REMINDER_URGENCY_ORDER)

private fun taskReminders(tasks: List<TaskRecord>, today: LocalDate): List<Reminder> =
    tasks
        .filter { task -> task.status != TaskStatus.DONE }
        .mapNotNull { task ->
            task.dueDate?.let { dueDate ->
                taskReminderKind(today.daysUntil(dueDate))?.let { reminderKind ->
                    Reminder(reminderKind, task.identifier, task.title, dueDate)
                }
            }
        }

private fun goalReminders(goals: List<GoalRecord>, today: LocalDate): List<Reminder> =
    goals
        .filterNot(GoalRecord::completed)
        .mapNotNull { goal ->
            goal.deadlineDate?.let { deadlineDate ->
                goalReminderKind(today.daysUntil(deadlineDate))?.let { reminderKind ->
                    Reminder(reminderKind, goal.identifier, goal.title, deadlineDate)
                }
            }
        }

private fun socialBatteryReminders(socialBatterySnapshot: SocialBatterySnapshot): List<Reminder> =
    listOfNotNull(
        Reminder(ReminderKind.SOCIAL_BATTERY_UNSET, SOCIAL_BATTERY_SUBJECT_IDENTIFIER, "", null)
            .takeIf { socialBatterySnapshot.socialBattery == null },
    )

private fun taskReminderKind(daysUntilDueDate: Int): ReminderKind? = when {
    daysUntilDueDate < 0 -> ReminderKind.TASK_OVERDUE
    daysUntilDueDate == 0 -> ReminderKind.TASK_DUE_TODAY
    daysUntilDueDate <= TASK_DUE_SOON_DAYS -> ReminderKind.TASK_DUE_SOON
    else -> null
}

private fun goalReminderKind(daysUntilDeadline: Int): ReminderKind? = when {
    daysUntilDeadline < 0 -> ReminderKind.GOAL_DEADLINE_PASSED
    daysUntilDeadline <= GOAL_DEADLINE_SOON_DAYS -> ReminderKind.GOAL_DEADLINE_SOON
    else -> null
}
