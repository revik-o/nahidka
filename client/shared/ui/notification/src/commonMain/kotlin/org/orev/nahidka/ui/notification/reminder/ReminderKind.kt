package org.orev.nahidka.ui.notification.reminder

import nahidka.shared.ui.notification.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

enum class ReminderKind(val destination: ApplicationDestination, val message: StringResource) {
    TASK_OVERDUE(ApplicationDestination.TASKS, Res.string.notification_task_overdue),
    GOAL_DEADLINE_PASSED(ApplicationDestination.GOALS, Res.string.notification_goal_deadline_passed),
    TASK_DUE_TODAY(ApplicationDestination.TASKS, Res.string.notification_task_due_today),
    TASK_DUE_SOON(ApplicationDestination.TASKS, Res.string.notification_task_due_soon),
    GOAL_DEADLINE_SOON(ApplicationDestination.GOALS, Res.string.notification_goal_deadline_soon),
    SOCIAL_BATTERY_UNSET(ApplicationDestination.SOCIAL_BATTERY, Res.string.notification_social_battery_unset),
}
