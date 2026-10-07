package org.orev.nahidka.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.orev.nahidka.navigation.ApplicationNavigator
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.notification.NotificationsButton
import org.orev.nahidka.ui.notification.NotificationsPopupButton
import org.orev.nahidka.ui.notification.NotificationsViewModel

@Composable
internal fun ApplicationShell(
    applicationNavigator: ApplicationNavigator,
    notificationsViewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    destinationContent: @Composable (ApplicationDestination) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(Modifier.safeDrawingPadding()) {
            when (LayoutWidth.of(maxWidth)) {
                LayoutWidth.EXPANDED -> Row(Modifier.fillMaxSize()) {
                    NavigationSidebar(applicationNavigator.navigationDestination, applicationNavigator::open)
                    ShellContent(
                        applicationNavigator = applicationNavigator,
                        modifier = Modifier.weight(1f),
                        notificationsAction = { NotificationsPopupButton(notificationsViewModel, applicationNavigator::open) },
                        destinationContent = destinationContent,
                    )
                }

                LayoutWidth.COMPACT -> Column(Modifier.fillMaxSize()) {
                    ShellContent(
                        applicationNavigator = applicationNavigator,
                        modifier = Modifier.weight(1f),
                        notificationsAction = {
                            if (applicationNavigator.currentDestination != ApplicationDestination.NOTIFICATIONS) {
                                NotificationsButton(notificationsViewModel) {
                                    applicationNavigator.open(ApplicationDestination.NOTIFICATIONS)
                                }
                            }
                        },
                        destinationContent = destinationContent,
                    )
                    FloatingNavigationBar(applicationNavigator.navigationDestination, applicationNavigator::open)
                }
            }
        }
    }
}

@Composable
private fun ShellContent(
    applicationNavigator: ApplicationNavigator,
    modifier: Modifier,
    notificationsAction: @Composable () -> Unit,
    destinationContent: @Composable (ApplicationDestination) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxHeight()) {
        val contentLayoutWidth = LayoutWidth.of(maxWidth)

        Column(Modifier.fillMaxSize()) {
            ApplicationTopBar(
                destination = applicationNavigator.currentDestination,
                layoutWidth = contentLayoutWidth,
                onNavigateBack = applicationNavigator::navigateBack.takeIf { applicationNavigator.canNavigateBack },
            ) {
                notificationsAction()
            }
            Box(Modifier.weight(1f)) {
                destinationContent(applicationNavigator.currentDestination)
            }
        }
    }
}
