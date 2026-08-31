package org.orev.nahidka.ui.financialmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.TransactionEntity
import org.orev.nahidka.ui.common.WindowSize
import org.orev.nahidka.ui.common.rememberWindowSizeClass

@Composable
fun FinancialManagementTable(
    transactions: List<TransactionEntity>,
    selectMode: Boolean,
    onToggleSelect: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val windowSize = rememberWindowSizeClass(maxWidth)
        val isDesktop = windowSize == WindowSize.EXPANDED
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(transactions, key = { it.id }) { transaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectMode) {
                        Checkbox(
                            checked = transaction.isSelected,
                            onCheckedChange = { onToggleSelect(transaction) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Placeholder icon
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = transaction.iconName,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    val moneyColor = if (transaction.amount < 0) Color.Red else Color.Green
                    Text(
                        text = "${if (transaction.amount > 0) "+" else ""}${transaction.amount}",
                        color = moneyColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    if (isDesktop) {
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { onEdit(transaction) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDelete(transaction) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                Divider()
            }
        }
    }
}
