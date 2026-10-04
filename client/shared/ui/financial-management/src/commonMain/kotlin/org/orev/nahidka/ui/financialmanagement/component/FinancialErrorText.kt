package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_conflict
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_not_found
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialError

@Composable
internal fun financialErrorText(financialError: FinancialError): String = when (financialError) {
    is FinancialError.Validation -> financialError.message
    is FinancialError.NotFound -> stringResource(Res.string.financialmanagement_error_not_found)
    is FinancialError.OperationConflict,
    is FinancialError.CategoryConflict,
    is FinancialError.PlanningConflict -> stringResource(Res.string.financialmanagement_error_conflict)

    is FinancialError.CategoryInUse,
    is FinancialError.CommandIdentifierReused,
    FinancialError.SessionClosed,
    FinancialError.StorageUnavailable,
    FinancialError.Forbidden -> stringResource(Res.string.financialmanagement_error_unsaved)
}
