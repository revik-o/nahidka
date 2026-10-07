package org.orev.nahidka.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.common.generated.resources.icon_close
import nahidka.shared.ui.notification.generated.resources.Res
import nahidka.shared.ui.notification.generated.resources.notification_action_dismiss
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.component.DestinationIcon
import org.orev.nahidka.ui.notification.reminder.Reminder
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

private val REMINDER_ICON_CONTAINER_SIZE = 36.dp
private val REMINDER_ICON_SIZE = 20.dp
private val DISMISS_ICON_SIZE = 18.dp

@Composable
internal fun NotificationRow(
    reminder: Reminder,
    onReminderOpen: (Reminder) -> Unit,
    onReminderDismiss: (Reminder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReminderOpen(reminder) }
            .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(REMINDER_ICON_CONTAINER_SIZE)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            DestinationIcon(
                destination = reminder.kind.destination,
                modifier = Modifier.size(REMINDER_ICON_SIZE),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(reminder.kind.message, reminder.subjectTitle),
                style = MaterialTheme.typography.bodyMedium,
            )
            reminder.date?.let { reminderDate ->
                DateText(reminderDate)
            }
        }
        IconButton(onClick = { onReminderDismiss(reminder) }) {
            Icon(
                painter = painterResource(CommonResources.drawable.icon_close),
                contentDescription = stringResource(Res.string.notification_action_dismiss),
                modifier = Modifier.size(DISMISS_ICON_SIZE),
            )
        }
    }
}
