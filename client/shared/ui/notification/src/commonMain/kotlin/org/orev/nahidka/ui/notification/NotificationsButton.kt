package org.orev.nahidka.ui.notification

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DestinationIcon
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

@Composable
fun NotificationsButton(
    notificationsViewModel: NotificationsViewModel,
    onClick: () -> Unit,
) {
    val reminders by notificationsViewModel.reminders.collectAsStateWithLifecycle()
    val notificationsTitle = stringResource(ApplicationDestination.NOTIFICATIONS.title)

    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = notificationsTitle },
    ) {
        BadgedBox(
            badge = {
                if (reminders.isNotEmpty()) {
                    Badge {
                        Text(reminders.size.toString())
                    }
                }
            },
        ) {
            DestinationIcon(ApplicationDestination.NOTIFICATIONS)
        }
    }
}
