package org.orev.nahidka.ui.notification

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.notification.generated.resources.Res
import nahidka.shared.ui.notification.generated.resources.notification_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.SupportingText
import org.orev.nahidka.ui.notification.reminder.Reminder

@Composable
internal fun NotificationList(
    notificationsViewModel: NotificationsViewModel,
    onReminderOpen: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reminders by notificationsViewModel.reminders.collectAsStateWithLifecycle()

    if (reminders.isEmpty()) {
        SupportingText(
            text = stringResource(Res.string.notification_list_empty),
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = TextAlign.Center,
        )
    } else {
        LazyColumn(modifier) {
            items(reminders, key = Reminder::identifier) { reminder ->
                NotificationRow(reminder, onReminderOpen, notificationsViewModel::dismiss)
            }
        }
    }
}
