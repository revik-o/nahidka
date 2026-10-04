package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_cancel
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.common.FinancialDialogController

@Composable
internal fun <Draft> FinancialDialog(
    financialDialogController: FinancialDialogController<Draft>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    content: @Composable (Draft) -> Unit,
) {
    val openedDialogState by financialDialogController.dialogState.collectAsStateWithLifecycle()
    val dialogState = openedDialogState ?: return

    AlertDialog(
        onDismissRequest = financialDialogController::dismiss,
        title = { Text(title(dialogState.draft)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content(dialogState.draft)
                dialogState.rejection?.let { rejection ->
                    Text(financialErrorText(rejection), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = financialDialogController::submit,
                enabled = dialogState.submittable && !dialogState.submitting,
            ) {
                Text(confirmationTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = financialDialogController::dismiss, enabled = !dialogState.submitting) {
                Text(stringResource(Res.string.financialmanagement_action_cancel))
            }
        },
    )
}
