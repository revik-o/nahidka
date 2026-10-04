package org.orev.nahidka.ui.financialmanagement

import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_tab_history
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_tab_planning
import org.jetbrains.compose.resources.StringResource

internal enum class FinancialManagementTab(val title: StringResource) {
    HISTORY(Res.string.financialmanagement_tab_history),
    PLANNING(Res.string.financialmanagement_tab_planning),
}
