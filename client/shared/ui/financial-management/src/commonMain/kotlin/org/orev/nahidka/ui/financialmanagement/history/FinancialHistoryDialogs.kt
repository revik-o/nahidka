package org.orev.nahidka.ui.financialmanagement.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.ui.financialmanagement.common.availableContentOrNull
import org.orev.nahidka.ui.financialmanagement.component.*

@Composable
internal fun FinancialHistoryDialogs(financialHistoryViewModel: FinancialHistoryViewModel) {
    val historyContentState by financialHistoryViewModel.history.collectAsStateWithLifecycle()
    val selectableCategories = historyContentState
        .availableContentOrNull()
        ?.categories
        .orEmpty()
        .filterNot(FinancialCategory::archived)

    FinancialDialog(
        financialDialogController = financialHistoryViewModel.operationEditor,
        title = { operationDraft ->
            stringResource(
                if (operationDraft.editedOperation == null) {
                    Res.string.financialmanagement_operation_creation_title
                } else {
                    Res.string.financialmanagement_operation_editing_title
                },
            )
        },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_save),
    ) { operationDraft ->
        FinancialOperationForm(
            operationDraft = operationDraft,
            selectableCategories = selectableCategories,
            onOperationDraftEdit = financialHistoryViewModel.operationEditor::edit,
        )
    }

    FinancialDialog(
        financialDialogController = financialHistoryViewModel.operationDeletion,
        title = { stringResource(Res.string.financialmanagement_operation_deletion_title) },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_delete),
    ) { operationRow ->
        Text(
            stringResource(
                Res.string.financialmanagement_operation_deletion_message,
                operationRow.categoryName,
                operationRow.formattedAmount,
            ),
        )
    }
}

@Composable
private fun FinancialOperationForm(
    operationDraft: FinancialOperationDraft,
    selectableCategories: List<FinancialCategory>,
    onOperationDraftEdit: ((FinancialOperationDraft) -> FinancialOperationDraft) -> Unit,
) {
    if (operationDraft.editedOperation == null) {
        FinancialChoiceSelector(
            options = FINANCIAL_OPERATION_CREATION_KINDS,
            selectedOption = operationDraft.kind,
            optionTitle = { operationKind -> stringResource(operationKind.title) },
            onOptionSelect = { operationKind -> onOperationDraftEdit { draft -> draft.copy(kind = operationKind) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    FinancialMoneyField(
        amountText = operationDraft.amountText,
        asset = operationDraft.asset,
        title = stringResource(Res.string.financialmanagement_field_amount),
        onAmountTextChange = { amountText -> onOperationDraftEdit { draft -> draft.copy(amountText = amountText) } },
    )
    FinancialCategoryField(
        selectedCategory = operationDraft.category,
        selectableCategories = selectableCategories,
        onCategorySelect = { category -> onOperationDraftEdit { draft -> draft.copy(category = category) } },
    )
    FinancialChoiceSelector(
        options = PaymentMethod.entries,
        selectedOption = operationDraft.paymentMethod,
        optionTitle = { paymentMethod -> stringResource(paymentMethod.title) },
        onOptionSelect = { paymentMethod -> onOperationDraftEdit { draft -> draft.copy(paymentMethod = paymentMethod) } },
        modifier = Modifier.fillMaxWidth(),
    )
    FinancialDateField(
        date = operationDraft.occurredOn,
        onDateChange = { occurredOn -> onOperationDraftEdit { draft -> draft.copy(occurredOn = occurredOn) } },
    )
}
