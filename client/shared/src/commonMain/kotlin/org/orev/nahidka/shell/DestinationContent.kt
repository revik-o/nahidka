package org.orev.nahidka.shell

import androidx.compose.runtime.Composable
import org.orev.nahidka.di.ApplicationSessionOwner
import org.orev.nahidka.di.sessionViewModel
import org.orev.nahidka.navigation.ApplicationNavigator
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.financialmanagement.FinancialManagementScreen
import org.orev.nahidka.ui.goal.GoalsScreen
import org.orev.nahidka.ui.notification.NotificationsScreen
import org.orev.nahidka.ui.settings.SettingsScreen
import org.orev.nahidka.ui.socialbattery.SocialBatteryScreen
import org.orev.nahidka.ui.tasks.TasksScreen

@Composable
internal fun DestinationContent(
    destination: ApplicationDestination,
    applicationSessionOwner: ApplicationSessionOwner,
    applicationNavigator: ApplicationNavigator,
) {
    when (destination) {
        ApplicationDestination.DASHBOARD -> DashboardScreen(
            dashboardViewModel = applicationSessionOwner.sessionViewModel { dashboardViewModel },
            socialBatteryViewModel = applicationSessionOwner.sessionViewModel { socialBatteryViewModel },
            tasksViewModel = applicationSessionOwner.sessionViewModel { tasksViewModel },
            goalsViewModel = applicationSessionOwner.sessionViewModel { goalsViewModel },
            financialOverviewViewModel = applicationSessionOwner.sessionViewModel { financialOverviewViewModel },
            onDestinationOpen = applicationNavigator::open,
        )

        ApplicationDestination.SOCIAL_BATTERY -> SocialBatteryScreen(
            applicationSessionOwner.sessionViewModel { socialBatteryViewModel },
        )

        ApplicationDestination.TASKS -> TasksScreen(
            applicationSessionOwner.sessionViewModel { tasksViewModel },
        )

        ApplicationDestination.GOALS -> GoalsScreen(
            applicationSessionOwner.sessionViewModel { goalsViewModel },
        )

        ApplicationDestination.SETTINGS -> SettingsScreen(
            applicationSessionOwner.sessionViewModel { settingsViewModel },
        )

        ApplicationDestination.FINANCE -> FinancialManagementScreen(
            applicationSessionOwner.sessionViewModel { financialHistoryViewModel },
            applicationSessionOwner.sessionViewModel { financialPlanningViewModel },
        )

        ApplicationDestination.NOTIFICATIONS -> NotificationsScreen(
            applicationSessionOwner.sessionViewModel { notificationsViewModel },
            applicationNavigator::open,
        )
    }
}
