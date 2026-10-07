package org.orev.nahidka.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_back
import nahidka.shared.ui.common.generated.resources.icon_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val APPLICATION_TOP_BAR_HEIGHT = 64.dp

@Composable
internal fun ApplicationTopBar(
    destination: ApplicationDestination,
    layoutWidth: LayoutWidth,
    onNavigateBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(APPLICATION_TOP_BAR_HEIGHT)
            .padding(horizontal = layoutWidth.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onNavigateBack?.let { navigateBack ->
            IconButton(onClick = navigateBack) {
                Icon(
                    painter = painterResource(Res.drawable.icon_back),
                    contentDescription = stringResource(Res.string.common_action_back),
                )
            }
        }
        Text(
            text = stringResource(destination.title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
