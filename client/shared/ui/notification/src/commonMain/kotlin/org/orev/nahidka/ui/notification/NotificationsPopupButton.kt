package org.orev.nahidka.ui.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val NOTIFICATIONS_POPUP_OFFSET = 48.dp
private val NOTIFICATIONS_POPUP_WIDTH = 380.dp
private val NOTIFICATIONS_POPUP_MAXIMUM_HEIGHT = 480.dp

@Composable
fun NotificationsPopupButton(
    notificationsViewModel: NotificationsViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
) {
    var popupVisible by remember { mutableStateOf(false) }
    val popupOffset = with(LocalDensity.current) { NOTIFICATIONS_POPUP_OFFSET.roundToPx() }

    Box {
        NotificationsButton(notificationsViewModel) { popupVisible = !popupVisible }
        if (popupVisible) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, popupOffset),
                onDismissRequest = { popupVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier
                        .width(NOTIFICATIONS_POPUP_WIDTH)
                        .heightIn(max = NOTIFICATIONS_POPUP_MAXIMUM_HEIGHT),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                ) {
                    Column {
                        Text(
                            text = stringResource(ApplicationDestination.NOTIFICATIONS.title),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        HorizontalDivider()
                        NotificationList(
                            notificationsViewModel = notificationsViewModel,
                            onReminderOpen = { reminder ->
                                popupVisible = false
                                onDestinationOpen(reminder.kind.destination)
                            },
                        )
                    }
                }
            }
        }
    }
}
