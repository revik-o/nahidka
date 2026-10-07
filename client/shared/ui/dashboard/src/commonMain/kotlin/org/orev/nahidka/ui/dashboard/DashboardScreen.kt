package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.DateText
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.financialmanagement.overview.FinancialOverviewCard
import org.orev.nahidka.ui.financialmanagement.overview.FinancialOverviewViewModel
import org.orev.nahidka.ui.financialmanagement.overview.FinancialSpendingCard
import org.orev.nahidka.ui.goal.GoalsSummaryCard
import org.orev.nahidka.ui.goal.GoalsViewModel
import org.orev.nahidka.ui.socialbattery.SocialBatterySummaryCard
import org.orev.nahidka.ui.socialbattery.SocialBatteryViewModel
import org.orev.nahidka.ui.tasks.TasksSummaryCard
import org.orev.nahidka.ui.tasks.TasksViewModel

private val DASHBOARD_CARD_MINIMUM_WIDTH = 300.dp
private const val DASHBOARD_MAXIMUM_COLUMN_COUNT = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    socialBatteryViewModel: SocialBatteryViewModel,
    tasksViewModel: TasksViewModel,
    goalsViewModel: GoalsViewModel,
    financialOverviewViewModel: FinancialOverviewViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboardHeader by dashboardViewModel.header.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val columnCount = (maxWidth / DASHBOARD_CARD_MINIMUM_WIDTH)
            .toInt()
            .coerceIn(1, DASHBOARD_MAXIMUM_COLUMN_COUNT)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LayoutWidth.of(maxWidth).screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(dashboardHeader.dayPeriod.greeting),
                    style = MaterialTheme.typography.headlineSmall,
                )
                DateText(dashboardHeader.today)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = columnCount,
            ) {
                val cardModifier = Modifier
                    .weight(1f)
                    .fillMaxRowHeight()

                SocialBatterySummaryCard(socialBatteryViewModel, onDestinationOpen, cardModifier)
                TasksSummaryCard(tasksViewModel, onDestinationOpen, cardModifier)
                GoalsSummaryCard(goalsViewModel, onDestinationOpen, cardModifier)
                FinancialOverviewCard(financialOverviewViewModel, onDestinationOpen, cardModifier)
                FinancialSpendingCard(financialOverviewViewModel, onDestinationOpen, cardModifier)
            }
        }
    }
}
