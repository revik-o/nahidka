package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

private val SUMMARY_CARD_ICON_SIZE = 20.dp

@Composable
fun SummaryCard(
    destination: ApplicationDestination,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(destination.title),
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        onClick = { onDestinationOpen(destination) },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DestinationIcon(
                    destination = destination,
                    modifier = Modifier.size(SUMMARY_CARD_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content()
        }
    }
}
