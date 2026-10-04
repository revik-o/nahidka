package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun <Option> ChoiceSelector(
    options: List<Option>,
    selectedOption: Option,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { optionIndex, option ->
            SegmentedButton(
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(optionIndex, options.size),
            ) {
                Text(
                    text = optionTitle(option),
                    modifier = Modifier.width(IntrinsicSize.Max),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
