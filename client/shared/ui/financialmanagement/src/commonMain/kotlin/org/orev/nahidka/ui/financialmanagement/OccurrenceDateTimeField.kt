package org.orev.nahidka.ui.financialmanagement

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.datetime.*
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OccurrenceDateTimeField(
    occurredAt: Instant,
    reportingTimeZone: TimeZone,
    onChange: (Instant) -> Unit,
) = key(occurredAt) {
    val localDateTime = occurredAt.toLocalDateTime(reportingTimeZone)
    var showingDate by remember { mutableStateOf(false) }
    var showingTime by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(localDateTime.date) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = localDateTime.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )
    val timeState = rememberTimePickerState(initialHour = localDateTime.hour, initialMinute = localDateTime.minute)

    TextButton(onClick = { showingDate = true }) {
        Text("Date and time: ${localDateTime.date} ${localDateTime.time.hour.toString().padStart(2, '0')}:${localDateTime.time.minute.toString().padStart(2, '0')}")
    }
    if (showingDate) {
        DatePickerDialog(
            onDismissRequest = { showingDate = false },
            confirmButton = {
                TextButton(enabled = dateState.selectedDateMillis != null, onClick = {
                    selectedDate = Instant.fromEpochMilliseconds(requireNotNull(dateState.selectedDateMillis)).toLocalDateTime(TimeZone.UTC).date
                    showingDate = false
                    showingTime = true
                }) { Text("Next: time") }
            },
            dismissButton = { TextButton(onClick = { showingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }
    if (showingTime) {
        AlertDialog(
            onDismissRequest = { showingTime = false },
            title = { Text("Occurrence time · ${reportingTimeZone.id}") },
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(selectedDate.atTime(timeState.hour, timeState.minute).toInstant(reportingTimeZone))
                    showingTime = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showingTime = false }) { Text("Cancel") } },
        )
    }
}
