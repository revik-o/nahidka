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

private val APPLICATION_TOP_BAR_HEIGHT = 64.dp

@Composable
fun ApplicationTopBar(
    applicationTopBarState: ApplicationTopBarState,
    modifier: Modifier = Modifier
        .height(APPLICATION_TOP_BAR_HEIGHT)
        .padding(end = applicationTopBarState.layoutWidth.screenPadding),
    captionContainer: @Composable (Modifier, @Composable () -> Unit) -> Unit = { captionModifier, captionContent ->
        Box(captionModifier) { captionContent() }
    },
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        applicationTopBarState.onNavigateBack?.let { navigateBack ->
            IconButton(
                onClick = navigateBack,
                modifier = Modifier.padding(start = applicationTopBarState.layoutWidth.screenPadding),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_back),
                    contentDescription = stringResource(Res.string.common_action_back),
                )
            }
        }
        captionContainer(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = applicationTopBarState.title,
                    modifier = Modifier.padding(
                        start = if (applicationTopBarState.onNavigateBack == null) {
                            applicationTopBarState.layoutWidth.screenPadding
                        } else {
                            8.dp
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}
