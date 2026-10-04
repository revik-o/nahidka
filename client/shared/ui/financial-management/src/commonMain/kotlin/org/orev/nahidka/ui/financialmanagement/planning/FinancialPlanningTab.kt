package org.orev.nahidka.ui.financialmanagement.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialPlanningTab(
    financialPlanningViewModel: FinancialPlanningViewModel,
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
) {
    val selectedMonth by financialPlanningViewModel.selectedMonth.collectAsStateWithLifecycle()
    val planningContentState by financialPlanningViewModel.planning.collectAsStateWithLifecycle()

    FinancialTabLayout(
        financialLayoutWidth = financialLayoutWidth,
        tabSelector = tabSelector,
        actions = {
            FinancialMonthSelector(
                selectedMonth = selectedMonth,
                onPreviousMonthClick = financialPlanningViewModel::selectPreviousMonth,
                onNextMonthClick = financialPlanningViewModel::selectNextMonth,
            )
            FinancialAddButton(
                title = stringResource(Res.string.financialmanagement_planning_add),
                financialLayoutWidth = financialLayoutWidth,
                onClick = financialPlanningViewModel::openPlanningRowCreation,
            )
        },
    ) {
        FinancialContent(planningContentState) { planningContent ->
            FinancialPlanningSummaryText(planningContent.summary)
            FinancialTable(
                rows = planningContent.rows,
                rowKey = { planningRow -> planningRow.planningRow.identifier },
                emptyTableMessage = stringResource(Res.string.financialmanagement_planning_empty),
            ) { planningRow ->
                FinancialTableRow(
                    financialTableEntry = planningRow,
                    financialLayoutWidth = financialLayoutWidth,
                    onEditClick = { financialPlanningViewModel.openPlanningRowEditing(planningRow) },
                    onDeleteClick = { financialPlanningViewModel.openPlanningRowDeletion(planningRow) },
                )
            }
        }
    }
}

@Composable
private fun FinancialPlanningSummaryText(financialPlanningSummary: FinancialPlanningSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.financialmanagement_planning_available, financialPlanningSummary.formattedAvailableAmount),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.financialmanagement_planning_remaining, financialPlanningSummary.formattedRemainingAmount),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.financialmanagement_planning_savable, financialPlanningSummary.formattedSavableAmount),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
