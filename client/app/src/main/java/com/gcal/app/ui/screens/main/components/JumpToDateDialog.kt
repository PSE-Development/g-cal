package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * JumpToDateDialog
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpToDateDialog(
    currentDate: LocalDate,
    onConfirmDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedDate by remember { mutableStateOf(currentDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calendar_jump_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))


                Text(
                    text = stringResource(R.string.calendar_selected_date),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))


                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedDate.format(dateFormatter),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                Text(
                    text = stringResource(R.string.calendar_quick_select),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = selectedDate == LocalDate.now(),
                        onClick = { selectedDate = LocalDate.now() },
                        label = { Text(stringResource(R.string.calendar_today)) },
                        modifier = Modifier.weight(1f)
                    )


                    FilterChip(
                        selected = selectedDate == LocalDate.now().plusDays(1),
                        onClick = { selectedDate = LocalDate.now().plusDays(1) },
                        label = { Text(stringResource(R.string.calendar_tomorrow)) },
                        modifier = Modifier.weight(1f)
                    )


                    FilterChip(
                        selected = selectedDate == LocalDate.now().plusWeeks(1),
                        onClick = { selectedDate = LocalDate.now().plusWeeks(1) },
                        label = { Text(stringResource(R.string.calendar_next_week)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                Button(
                    onClick = { onConfirmDate(selectedDate) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.calendar_go_to_date),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}