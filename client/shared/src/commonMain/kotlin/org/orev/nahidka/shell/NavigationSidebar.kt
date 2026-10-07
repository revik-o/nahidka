package org.orev.nahidka.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nahidka.shared.generated.resources.Res
import nahidka.shared.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.BrandMark
import org.orev.nahidka.ui.common.component.DestinationIcon
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val NAVIGATION_SIDEBAR_WIDTH = 240.dp
private val SIDEBAR_BRAND_MARK_SIZE = 36.dp

@Composable
internal fun NavigationSidebar(
    selectedDestination: ApplicationDestination,
    onDestinationSelect: (ApplicationDestination) -> Unit,
    navigationSidebarTopInset: Dp = 0.dp,
) {
    Surface(
        modifier = Modifier
            .width(NAVIGATION_SIDEBAR_WIDTH)
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .padding(top = navigationSidebarTopInset)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(Modifier.size(SIDEBAR_BRAND_MARK_SIZE))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ApplicationDestination.NAVIGATION_DESTINATIONS.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.title)) },
                    selected = destination == selectedDestination,
                    onClick = { onDestinationSelect(destination) },
                    icon = { DestinationIcon(destination) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = NavigationSelectionColors.container,
                        selectedIconColor = NavigationSelectionColors.content,
                        selectedTextColor = NavigationSelectionColors.content,
                    ),
                )
            }
        }
    }
    VerticalDivider()
}
