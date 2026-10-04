package org.orev.nahidka.ui.financialmanagement

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.component.FinancialChoiceSelector
import org.orev.nahidka.ui.financialmanagement.component.FinancialLayoutWidth
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryDialogs
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryTab
import org.orev.nahidka.ui.financialmanagement.history.FinancialHistoryViewModel
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningDialogs
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningTab
import org.orev.nahidka.ui.financialmanagement.planning.FinancialPlanningViewModel

@Composable
fun FinancialManagementScreen(
    financialHistoryViewModel: FinancialHistoryViewModel,
    financialPlanningViewModel: FinancialPlanningViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(FinancialManagementTab.HISTORY) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val financialLayoutWidth = FinancialLayoutWidth.of(maxWidth)
        val tabSelector = @Composable {
            FinancialChoiceSelector(
                options = FinancialManagementTab.entries,
                selectedOption = selectedTab,
                optionTitle = { financialManagementTab -> stringResource(financialManagementTab.title) },
                onOptionSelect = { financialManagementTab -> selectedTab = financialManagementTab },
                modifier = if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) Modifier.fillMaxWidth() else Modifier,
            )
        }

        when (selectedTab) {
            FinancialManagementTab.HISTORY -> FinancialHistoryTab(financialHistoryViewModel, financialLayoutWidth, tabSelector)
            FinancialManagementTab.PLANNING -> FinancialPlanningTab(financialPlanningViewModel, financialLayoutWidth, tabSelector)
        }
    }

    FinancialHistoryDialogs(financialHistoryViewModel)
    FinancialPlanningDialogs(financialPlanningViewModel)
}
