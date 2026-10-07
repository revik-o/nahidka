package org.orev.nahidka.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DestinationIcon
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val FLOATING_NAVIGATION_BAR_HEIGHT = 68.dp
private val FLOATING_NAVIGATION_BAR_SHAPE = RoundedCornerShape(28.dp)
private val FLOATING_NAVIGATION_BAR_OUTER_PADDING = 16.dp
private val FLOATING_NAVIGATION_BAR_CONTENT_PADDING = 4.dp
private val FLOATING_NAVIGATION_BAR_MINIMUM_ITEM_WIDTH = 48.dp
private val FLOATING_NAVIGATION_BAR_LABEL_COMFORTABLE_WIDTH = 56.dp

@Composable
internal fun FloatingNavigationBar(
    selectedDestination: ApplicationDestination,
    onDestinationSelect: (ApplicationDestination) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val navigationDestinations = ApplicationDestination.NAVIGATION_DESTINATIONS
        val minimumNavigationBarContentWidth = FLOATING_NAVIGATION_BAR_MINIMUM_ITEM_WIDTH * navigationDestinations.size +
            FLOATING_NAVIGATION_BAR_CONTENT_PADDING * 2
        val navigationBarHorizontalPadding = ((maxWidth - minimumNavigationBarContentWidth) / 2)
            .coerceIn(0.dp, FLOATING_NAVIGATION_BAR_OUTER_PADDING)
        val navigationBarItemWidth = (maxWidth - navigationBarHorizontalPadding * 2 - FLOATING_NAVIGATION_BAR_CONTENT_PADDING * 2) /
            navigationDestinations.size
        val navigationLabelTextStyle = if (navigationBarItemWidth < FLOATING_NAVIGATION_BAR_LABEL_COMFORTABLE_WIDTH) {
            MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)
        } else {
            MaterialTheme.typography.labelMedium
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = navigationBarHorizontalPadding,
                    end = navigationBarHorizontalPadding,
                    bottom = FLOATING_NAVIGATION_BAR_OUTER_PADDING,
                ),
            shape = FLOATING_NAVIGATION_BAR_SHAPE,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(FLOATING_NAVIGATION_BAR_HEIGHT)
                    .padding(horizontal = FLOATING_NAVIGATION_BAR_CONTENT_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selectedDestination,
                        onClick = { onDestinationSelect(destination) },
                        icon = { DestinationIcon(destination) },
                        label = {
                            Text(
                                text = stringResource(destination.navigationLabel),
                                style = navigationLabelTextStyle,
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
}
