package org.orev.nahidka.ui.financialmanagement.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_history_empty
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_history_end
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_add
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialHistoryTab(
    financialHistoryViewModel: FinancialHistoryViewModel,
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
) {
    val selectedAsset by financialHistoryViewModel.selectedAsset.collectAsStateWithLifecycle()
    val historyContentState by financialHistoryViewModel.history.collectAsStateWithLifecycle()

    FinancialTabLayout(
        financialLayoutWidth = financialLayoutWidth,
        tabSelector = tabSelector,
        actions = {
            FinancialChoiceSelector(
                options = financialHistoryViewModel.availableAssets,
                selectedOption = selectedAsset,
                optionTitle = { asset -> asset.displayCode },
                onOptionSelect = financialHistoryViewModel::selectAsset,
            )
            FinancialAddButton(
                title = stringResource(Res.string.financialmanagement_operation_add),
                financialLayoutWidth = financialLayoutWidth,
                onClick = financialHistoryViewModel::openOperationCreation,
            )
        },
    ) {
        FinancialContent(historyContentState) { historyContent ->
            FinancialTable(
                rows = historyContent.rows,
                rowKey = { operationRow -> operationRow.operation.identifier },
                emptyTableMessage = stringResource(Res.string.financialmanagement_history_empty),
                footer = { FinancialHistoryFooter(historyContent, financialHistoryViewModel::loadNextPage) },
            ) { operationRow ->
                FinancialTableRow(
                    financialTableEntry = operationRow,
                    financialLayoutWidth = financialLayoutWidth,
                    onEditClick = { financialHistoryViewModel.openOperationEditing(operationRow) },
                    onDeleteClick = { financialHistoryViewModel.openOperationDeletion(operationRow) },
                )
            }
        }
    }
}

@Composable
private fun FinancialHistoryFooter(historyContent: FinancialHistoryContent, onEndReached: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (historyContent.hasMoreRows) {
            LaunchedEffect(historyContent.rows.size) { onEndReached() }
            CircularProgressIndicator()
        } else {
            Text(stringResource(Res.string.financialmanagement_history_end), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
