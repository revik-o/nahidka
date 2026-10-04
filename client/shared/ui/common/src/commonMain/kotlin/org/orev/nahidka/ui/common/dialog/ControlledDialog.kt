package org.orev.nahidka.ui.common.dialog

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
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Draft, Rejection : Any> ControlledDialog(
    dialogController: DialogController<Draft, Rejection>,
    title: @Composable (Draft) -> String,
    confirmationTitle: String,
    rejectionText: @Composable (Rejection) -> String,
    content: @Composable (Draft) -> Unit,
) {
    val openedDialogState by dialogController.dialogState.collectAsStateWithLifecycle()
    val dialogState = openedDialogState ?: return

    AlertDialog(
        onDismissRequest = dialogController::dismiss,
        title = { Text(title(dialogState.draft)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content(dialogState.draft)
                dialogState.rejection?.let { rejection ->
                    Text(rejectionText(rejection), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = dialogController::submit,
                enabled = dialogState.submittable && !dialogState.submitting,
            ) {
                Text(confirmationTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = dialogController::dismiss) {
                Text(stringResource(Res.string.common_action_cancel))
            }
        },
    )
}
