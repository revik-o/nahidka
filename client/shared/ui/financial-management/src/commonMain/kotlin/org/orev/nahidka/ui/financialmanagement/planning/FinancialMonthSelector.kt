package org.orev.nahidka.ui.financialmanagement.planning

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_planning_next_month
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_planning_previous_month
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.FINANCIAL_MONTH_FORMAT

@Composable
internal fun FinancialMonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
) {
    val previousMonthTitle = stringResource(Res.string.financialmanagement_planning_previous_month)
    val nextMonthTitle = stringResource(Res.string.financialmanagement_planning_next_month)

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onPreviousMonthClick,
            modifier = Modifier.semantics { contentDescription = previousMonthTitle },
        ) {
            Text("‹")
        }
        Text(selectedMonth.format(FINANCIAL_MONTH_FORMAT), style = MaterialTheme.typography.titleMedium)
        IconButton(
            onClick = onNextMonthClick,
            modifier = Modifier.semantics { contentDescription = nextMonthTitle },
        ) {
            Text("›")
        }
    }
}
