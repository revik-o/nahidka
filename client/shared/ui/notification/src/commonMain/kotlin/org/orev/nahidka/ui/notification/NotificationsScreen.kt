package org.orev.nahidka.ui.notification

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

@Composable
fun NotificationsScreen(
    notificationsViewModel: NotificationsViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        NotificationList(
            notificationsViewModel = notificationsViewModel,
            onReminderOpen = { reminder -> onDestinationOpen(reminder.kind.destination) },
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = LayoutWidth.of(maxWidth).screenPadding),
        )
    }
}
