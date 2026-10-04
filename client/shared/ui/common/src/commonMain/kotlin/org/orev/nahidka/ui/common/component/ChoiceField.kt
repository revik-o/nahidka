package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Option> ChoiceField(
    title: String,
    options: List<Option>,
    selectedOption: Option,
    optionTitle: @Composable (Option) -> String,
    onOptionSelect: (Option) -> Unit,
) {
    var optionsExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = optionsExpanded, onExpandedChange = { expanded -> optionsExpanded = expanded }) {
        OutlinedTextField(
            value = optionTitle(selectedOption),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = optionsExpanded) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = optionsExpanded, onDismissRequest = { optionsExpanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionTitle(option)) },
                    onClick = {
                        onOptionSelect(option)
                        optionsExpanded = false
                    },
                )
            }
        }
    }
}
