package org.orev.nahidka.ui.common.component

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

@Composable
fun DestinationIcon(
    destination: ApplicationDestination,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(destination.icon),
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
