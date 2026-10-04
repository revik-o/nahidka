package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.*
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_cancel
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_action_select
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinancialDateField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var datePickerVisible by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { datePickerVisible = true }, modifier = Modifier.fillMaxWidth()) {
        Text("📅 ${date.format(FINANCIAL_DAY_FORMAT)}")
    }

    if (datePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )

        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMilliseconds ->
                            onDateChange(Instant.fromEpochMilliseconds(selectedDateMilliseconds).toLocalDateTime(TimeZone.UTC).date)
                        }
                        datePickerVisible = false
                    },
                ) {
                    Text(stringResource(Res.string.financialmanagement_action_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(stringResource(Res.string.financialmanagement_action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
