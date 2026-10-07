package org.orev.nahidka.ui.financialmanagement.overview

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orev.nahidka.ui.common.component.SummaryCard
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.financialmanagement.component.FinancialContent

@Composable
internal fun FinancialOverviewSummaryCard(
    financialOverviewViewModel: FinancialOverviewViewModel,
    title: String,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.(FinancialOverview) -> Unit,
) {
    val overviewState by financialOverviewViewModel.overview.collectAsStateWithLifecycle()

    SummaryCard(
        destination = ApplicationDestination.FINANCE,
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
        title = title,
    ) {
        FinancialContent(overviewState) { financialOverview ->
            content(financialOverview)
        }
    }
}
