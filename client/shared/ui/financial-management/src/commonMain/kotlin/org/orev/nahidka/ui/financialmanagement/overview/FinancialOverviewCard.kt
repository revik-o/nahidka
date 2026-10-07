package org.orev.nahidka.ui.financialmanagement.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.SupportingText
import org.orev.nahidka.ui.common.component.LabeledField
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

@Composable
fun FinancialOverviewCard(
    financialOverviewViewModel: FinancialOverviewViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FinancialOverviewSummaryCard(
        financialOverviewViewModel = financialOverviewViewModel,
        title = stringResource(ApplicationDestination.FINANCE.title),
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
    ) { financialOverview ->
        LabeledField(stringResource(Res.string.financialmanagement_overview_net_expense)) {
            Text(financialOverview.formattedNetExpense, style = MaterialTheme.typography.headlineSmall)
        }
        LabeledField(stringResource(Res.string.financialmanagement_overview_income)) {
            Text(
                text = financialOverview.formattedIncome,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        SupportingText(
            text = financialOverview.formattedAvailableAfterPlanning
                ?.let { formattedAvailableAfterPlanning ->
                    stringResource(Res.string.financialmanagement_overview_available_after_planning, formattedAvailableAfterPlanning)
                }
                ?: stringResource(Res.string.financialmanagement_overview_planning_missing),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
