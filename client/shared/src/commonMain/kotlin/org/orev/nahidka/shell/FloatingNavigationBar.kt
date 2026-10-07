package org.orev.nahidka.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DestinationIcon
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val FLOATING_NAVIGATION_BAR_HEIGHT = 68.dp
private val FLOATING_NAVIGATION_BAR_SHAPE = RoundedCornerShape(28.dp)

@Composable
internal fun FloatingNavigationBar(
    selectedDestination: ApplicationDestination,
    onDestinationSelect: (ApplicationDestination) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = FLOATING_NAVIGATION_BAR_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .height(FLOATING_NAVIGATION_BAR_HEIGHT)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ApplicationDestination.NAVIGATION_DESTINATIONS.forEach { destination ->
                NavigationBarItem(
                    selected = destination == selectedDestination,
                    onClick = { onDestinationSelect(destination) },
                    icon = { DestinationIcon(destination) },
                    label = {
                        Text(
                            text = stringResource(destination.navigationLabel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = NavigationSelectionColors.container,
                        selectedIconColor = NavigationSelectionColors.content,
                        selectedTextColor = NavigationSelectionColors.content,
                    ),
                )
            }
        }
    }
}
