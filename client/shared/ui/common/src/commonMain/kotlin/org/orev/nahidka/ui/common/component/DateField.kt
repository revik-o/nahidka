package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.datetime.*
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_cancel
import nahidka.shared.ui.common.generated.resources.common_action_clear
import nahidka.shared.ui.common.generated.resources.common_action_select
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.format.DAY_FORMAT
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate?,
    title: String,
    onDateSelect: (LocalDate) -> Unit,
    onDateClear: (() -> Unit)? = null,
) {
    var datePickerVisible by remember { mutableStateOf(false) }
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val clearTitle = stringResource(Res.string.common_action_clear)

    LaunchedEffect(fieldInteractionSource) {
        fieldInteractionSource.interactions
            .filterIsInstance<PressInteraction.Release>()
            .collect { datePickerVisible = true }
    }

    OutlinedTextField(
        value = date
            ?.format(DAY_FORMAT)
            .orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(title) },
        leadingIcon = { Text("📅") },
        trailingIcon = if (date != null && onDateClear != null) {
            {
                IconButton(onClick = onDateClear, modifier = Modifier.semantics { contentDescription = clearTitle }) {
                    Text("✕")
                }
            }
        } else {
            null
        },
        singleLine = true,
        interactionSource = fieldInteractionSource,
    )

    if (datePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                ?.atStartOfDayIn(TimeZone.UTC)
                ?.toEpochMilliseconds(),
        )

        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMilliseconds ->
                            onDateSelect(
                                Instant
                                    .fromEpochMilliseconds(selectedDateMilliseconds)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date,
                            )
                        }
                        datePickerVisible = false
                    },
                ) {
                    Text(stringResource(Res.string.common_action_select))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(stringResource(Res.string.common_action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
