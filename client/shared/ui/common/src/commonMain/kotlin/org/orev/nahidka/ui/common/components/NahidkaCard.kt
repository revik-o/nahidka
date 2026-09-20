package org.orev.nahidka.ui.common.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NahidkaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.large,
        content = content
    )
}

@Preview
@Composable
fun NahidkaCardPreview() {
    NahidkaTheme {
        NahidkaCard {
            Text("Card content")
        }
    }
}
