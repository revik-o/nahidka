package org.orev.nahidka.ui.notification.reminder

import kotlinx.datetime.LocalDate

data class Reminder(
    val kind: ReminderKind,
    val subjectIdentifier: String,
    val subjectTitle: String,
    val date: LocalDate?,
) {

    val identifier: String
        get() = "${kind.name}:$subjectIdentifier"
}
