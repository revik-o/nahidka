package org.orev.nahidka.ui.common.navigation

import nahidka.shared.ui.common.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class ApplicationDestination(
    val title: StringResource,
    val navigationLabel: StringResource,
    val icon: DrawableResource,
    val shownInNavigation: Boolean,
) {
    DASHBOARD(
        title = Res.string.destination_dashboard,
        navigationLabel = Res.string.destination_dashboard,
        icon = Res.drawable.icon_dashboard,
        shownInNavigation = true,
    ),
    SOCIAL_BATTERY(
        title = Res.string.destination_social_battery,
        navigationLabel = Res.string.destination_social_battery_label,
        icon = Res.drawable.icon_social_battery,
        shownInNavigation = true,
    ),
    TASKS(
        title = Res.string.destination_tasks,
        navigationLabel = Res.string.destination_tasks,
        icon = Res.drawable.icon_tasks,
        shownInNavigation = true,
    ),
    GOALS(
        title = Res.string.destination_goals,
        navigationLabel = Res.string.destination_goals,
        icon = Res.drawable.icon_goals,
        shownInNavigation = true,
    ),
    SETTINGS(
        title = Res.string.destination_settings,
        navigationLabel = Res.string.destination_settings_label,
        icon = Res.drawable.icon_settings,
        shownInNavigation = true,
    ),
    FINANCE(
        title = Res.string.destination_finance,
        navigationLabel = Res.string.destination_finance,
        icon = Res.drawable.icon_finance,
        shownInNavigation = false,
    ),
    NOTIFICATIONS(
        title = Res.string.destination_notifications,
        navigationLabel = Res.string.destination_notifications,
        icon = Res.drawable.icon_notifications,
        shownInNavigation = false,
    );

    companion object {

        val NAVIGATION_DESTINATIONS: List<ApplicationDestination> =
            entries.filter(ApplicationDestination::shownInNavigation)
    }
}
