package org.orev.nahidka.ui.financialmanagement

import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.TransactionEntity

@Composable
fun FinancialManagementTable(
    transactions: List<TransactionEntity>,
    selectedIdentifiers: Set<String>,
    selectMode: Boolean,
    onToggleSelect: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(transactions, key = { it.identifier }) { transaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectMode) {
                        Checkbox(
                            checked = transaction.identifier in selectedIdentifiers,
                            onCheckedChange = { onToggleSelect(transaction) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Placeholder icon
                    Text(
                        text = "🛒",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    val moneyColor = if (transaction.isOutflow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    Text(
                        text = transaction.formattedAmount,
                        color = moneyColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { onEdit(transaction) }, modifier = Modifier.semantics { contentDescription = "Edit ${transaction.title}" }) {
                        Text("✎")
                    }
                    IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.semantics { contentDescription = "Delete ${transaction.title}" }) {
                        Text("×")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
fun FinancialManagementTablePreview() {
    NahidkaTheme {
        FinancialManagementTable(
            transactions = listOf(
                TransactionEntity(identifier = "1", version = 1, title = "Groceries", formattedAmount = "-50.00 USD", isOutflow = true, iconName = "cart"),
                TransactionEntity(identifier = "2", version = 1, title = "Salary", formattedAmount = "+1,000.00 USD", isOutflow = false, iconName = "money")
            ),
            selectedIdentifiers = emptySet(),
            selectMode = false,
            onToggleSelect = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
