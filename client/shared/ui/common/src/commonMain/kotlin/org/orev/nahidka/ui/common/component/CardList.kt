package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun <Item> CardList(
    listItems: List<Item>,
    itemKey: (Item) -> Any,
    emptyListMessage: String,
    modifier: Modifier = Modifier,
    itemCard: @Composable (Item) -> Unit,
) {
    if (listItems.isEmpty()) {
        Text(
            text = emptyListMessage,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listItems, key = itemKey) { listItem ->
                itemCard(listItem)
            }
        }
    }
}
