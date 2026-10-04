package org.orev.nahidka.ui.financialmanagement.planning

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
internal fun FinancialPlanningDialogs(financialPlanningViewModel: FinancialPlanningViewModel) {
    val planningContentState by financialPlanningViewModel.planning.collectAsStateWithLifecycle()
    val categories = planningContentState.availableContentOrNull()?.categories.orEmpty()

    FinancialDialog(
        financialDialogController = financialPlanningViewModel.planningRowEditor,
        title = { planningRowDraft ->
            stringResource(
                if (planningRowDraft.creation) {
                    Res.string.financialmanagement_planning_creation_title
                } else {
                    Res.string.financialmanagement_planning_editing_title
                },
            )
        },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_save),
    ) { planningRowDraft ->
        FinancialPlanningRowForm(
            planningRowDraft = planningRowDraft,
            selectableCategories = planningRowDraft.selectableCategories(categories),
            onPlanningRowDraftEdit = financialPlanningViewModel.planningRowEditor::edit,
        )
    }

    FinancialDialog(
        financialDialogController = financialPlanningViewModel.planningRowDeletion,
        title = { stringResource(Res.string.financialmanagement_planning_deletion_title) },
        confirmationTitle = stringResource(Res.string.financialmanagement_action_delete),
    ) { planningRowDraft ->
        Text(stringResource(Res.string.financialmanagement_planning_deletion_message, planningRowDraft.category?.name.orEmpty()))
    }
}

@Composable
private fun FinancialPlanningRowForm(
    planningRowDraft: FinancialPlanningRowDraft,
    selectableCategories: List<FinancialCategory>,
    onPlanningRowDraftEdit: ((FinancialPlanningRowDraft) -> FinancialPlanningRowDraft) -> Unit,
) {
    FinancialCategoryField(
        selectedCategory = planningRowDraft.category,
        selectableCategories = selectableCategories,
        onCategorySelect = { category -> onPlanningRowDraftEdit { draft -> draft.copy(category = category) } },
    )
    FinancialMoneyField(
        amountText = planningRowDraft.plannedAmountText,
        asset = planningRowDraft.asset,
        title = stringResource(Res.string.financialmanagement_field_planned_amount),
        onAmountTextChange = { plannedAmountText -> onPlanningRowDraftEdit { draft -> draft.copy(plannedAmountText = plannedAmountText) } },
    )
    FinancialChoiceSelector(
        options = PaymentMethod.entries,
        selectedOption = planningRowDraft.preferredPaymentMethod,
        optionTitle = { paymentMethod -> stringResource(paymentMethod.title) },
        onOptionSelect = { paymentMethod -> onPlanningRowDraftEdit { draft -> draft.copy(preferredPaymentMethod = paymentMethod) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
