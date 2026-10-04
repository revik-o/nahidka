package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_delete
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_edit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FinancialTableRow(
    financialTableEntry: FinancialTableEntry,
    financialLayoutWidth: FinancialLayoutWidth,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinancialCategoryIcon(financialTableEntry.categoryIconName, financialTableEntry.categoryName)
        when (financialLayoutWidth) {
            FinancialLayoutWidth.COMPACT -> {
                Column(Modifier.weight(1f)) {
                    Text(financialTableEntry.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    financialTableEntry.paymentMethod?.let { paymentMethod ->
                        FinancialEntrySupportingText(stringResource(paymentMethod.title))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    FinancialEntryAmount(financialTableEntry)
                    financialTableEntry.occurredOn?.let { occurredOn ->
                        FinancialEntrySupportingText(occurredOn.format(FINANCIAL_DAY_FORMAT))
                    }
                }
                FinancialEntryActionsMenu(onEditClick, onDeleteClick)
            }

            FinancialLayoutWidth.EXPANDED -> {
                Text(
                    text = financialTableEntry.categoryName,
                    modifier = Modifier.weight(2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FinancialEntryAmount(financialTableEntry, Modifier.weight(1.5f))
                Text(
                    text = financialTableEntry.paymentMethod?.let { paymentMethod -> stringResource(paymentMethod.title) }.orEmpty(),
                    modifier = Modifier.weight(1f),
                )
                financialTableEntry.occurredOn?.let { occurredOn ->
                    Text(occurredOn.format(FINANCIAL_DAY_FORMAT), Modifier.weight(1f))
                }
                FinancialEntryActionButtons(onEditClick, onDeleteClick)
            }
        }
    }
}

@Composable
private fun FinancialEntryAmount(financialTableEntry: FinancialTableEntry, modifier: Modifier = Modifier) {
    val amountDirection = financialTableEntry.amountDirection

    Text(
        text = when (amountDirection) {
            FinancialAmountDirection.INCOMING -> "↑ ${financialTableEntry.formattedAmount}"
            FinancialAmountDirection.OUTGOING -> "↓ ${financialTableEntry.formattedAmount}"
            null -> financialTableEntry.formattedAmount
        },
        modifier = modifier,
        color = when (amountDirection) {
            FinancialAmountDirection.INCOMING -> MaterialTheme.colorScheme.primary
            FinancialAmountDirection.OUTGOING -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.onSurface
        },
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun FinancialEntrySupportingText(supportingText: String) {
    Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FinancialEntryActionButtons(onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val editTitle = stringResource(Res.string.financialmanagement_action_edit)
    val deleteTitle = stringResource(Res.string.financialmanagement_action_delete)

    IconButton(onClick = onEditClick, modifier = Modifier.semantics { contentDescription = editTitle }) {
        Text("✎")
    }
    IconButton(onClick = onDeleteClick, modifier = Modifier.semantics { contentDescription = deleteTitle }) {
        Text("🗑")
    }
}

@Composable
private fun FinancialEntryActionsMenu(onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var actionsMenuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { actionsMenuExpanded = true }) {
            Text("⋮")
        }
        DropdownMenu(expanded = actionsMenuExpanded, onDismissRequest = { actionsMenuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.financialmanagement_action_edit)) },
                onClick = {
                    actionsMenuExpanded = false
                    onEditClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.financialmanagement_action_delete)) },
                onClick = {
                    actionsMenuExpanded = false
                    onDeleteClick()
                },
            )
        }
    }
}
