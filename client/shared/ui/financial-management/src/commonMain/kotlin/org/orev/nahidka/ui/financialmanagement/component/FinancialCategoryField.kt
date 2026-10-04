package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_field_category
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_field_category_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinancialCategoryField(
    selectedCategory: FinancialCategory?,
    selectableCategories: List<FinancialCategory>,
    onCategorySelect: (FinancialCategory) -> Unit,
) {
    var categoriesExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = categoriesExpanded, onExpandedChange = { expanded -> categoriesExpanded = expanded }) {
        OutlinedTextField(
            value = selectedCategory?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(stringResource(Res.string.financialmanagement_field_category)) },
            leadingIcon = selectedCategory?.let { category ->
                @Composable { FinancialCategoryIcon(category.iconName, category.name) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
            supportingText = {
                if (selectableCategories.isEmpty()) {
                    Text(stringResource(Res.string.financialmanagement_field_category_empty))
                }
            },
            singleLine = true,
        )
        if (selectableCategories.isNotEmpty()) {
            ExposedDropdownMenu(expanded = categoriesExpanded, onDismissRequest = { categoriesExpanded = false }) {
                selectableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        leadingIcon = { FinancialCategoryIcon(category.iconName, category.name) },
                        onClick = {
                            onCategorySelect(category)
                            categoriesExpanded = false
                        },
                    )
                }
            }
        }
    }
}
