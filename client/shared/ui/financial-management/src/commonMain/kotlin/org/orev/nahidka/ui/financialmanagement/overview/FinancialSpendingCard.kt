package org.orev.nahidka.ui.financialmanagement.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.SupportingText
import org.orev.nahidka.ui.common.navigation.ApplicationDestination

@Composable
fun FinancialSpendingCard(
    financialOverviewViewModel: FinancialOverviewViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FinancialOverviewSummaryCard(
        financialOverviewViewModel = financialOverviewViewModel,
        title = stringResource(Res.string.financialmanagement_spending_title),
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
    ) { financialOverview ->
        SupportingText(
            text = "${financialOverview.month} · ${financialOverview.assetDisplayCode}",
            style = MaterialTheme.typography.bodySmall,
        )
        FinancialSpendingDonut(financialOverview)
        financialOverview.formattedRefundCredits?.let { formattedRefundCredits ->
            SupportingText(
                text = stringResource(Res.string.financialmanagement_spending_refund_credits, formattedRefundCredits),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
