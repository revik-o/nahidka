package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <Option> ChoiceChips(
    options: List<Option>,
    selectedOption: Option?,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option == selectedOption

            FilterChip(
                selected = selected,
                onClick = { onOptionSelect(option.takeUnless { selected }) },
                label = { Text(optionTitle(option), style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
