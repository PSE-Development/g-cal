package com.gcal.app.ui.screens.berichte

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gcal.app.ui.components.GCalBottomBar
import com.gcal.app.ui.components.GCalTopBar
import com.gcal.app.ui.view_model.reportViewModel.DateRangePreset
import com.gcal.app.ui.view_model.reportViewModel.ReportUiEvent
import com.gcal.app.ui.view_model.reportViewModel.ReportUiState
import com.gcal.app.ui.view_model.reportViewModel.ReportViewModel
import com.gcal.app.ui.view_model.reportViewModel.UiErrorType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.res.stringResource
import com.gcal.app.R
import androidx.compose.ui.platform.LocalContext

/**
 * BerichteScreenView — Main view for the Reports tab.
 *
 *
 * @param viewModel The ReportViewModel (provided by GCalNavHost).
 * @param currentRoute The current route for BottomBar highlighting.
 * @param onBackClicked Callback for back navigation to MainScreen.
 * @param onNavigate Callback for tab navigation.
 * @param modifier Modifier for the screen container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerichteScreenView(
    viewModel: ReportViewModel,
    currentRoute: String?,
    onBackClicked: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()

    // Local state for date picker dialogs
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    // Snackbar for error messages
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Show non-validation errors as snackbar messages with retry action
    LaunchedEffect(uiState.activeError) {
        uiState.activeError?.let { error ->
            if (error !is UiErrorType.Validation) {
                val message = when (error) {
                    is UiErrorType.Network -> error.message
                    is UiErrorType.Unknown -> context.getString(R.string.error_unknown_occurred)
                    else -> context.getString(R.string.error_generic)
                }
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = context.getString(R.string.error_retry_short),
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    viewModel.onEvent(ReportUiEvent.OnRetryClicked)
                }
                viewModel.onEvent(ReportUiEvent.ErrorDismissed)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GCalTopBar(
                title = stringResource(R.string.reports_title),
                onBackClicked = onBackClicked
            )
        },
        bottomBar = {
            GCalBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content: date range selection + search button
            BerichteContent(
                state = uiState,
                onPresetSelected = { preset ->
                    viewModel.onEvent(ReportUiEvent.OnPresetSelected(preset))
                },
                onFromDateClicked = {
                    showFromDatePicker = true
                },
                onToDateClicked = {
                    showToDatePicker = true
                },
                onSearchClicked = {
                    viewModel.onEvent(ReportUiEvent.OnSearchClicked)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Result dialog (overlay).

            if (uiState.showResultDialog && uiState.reportResult != null) {
                ReportResultDialog(
                    data = uiState.reportResult!!,
                    onCloseClicked = {
                        viewModel.onEvent(ReportUiEvent.OnCloseDialogClicked)
                    }
                )
            }

            // "From" date picker dialog
            if (showFromDatePicker) {
                DatePickerDialogWrapper(
                    initialDate = uiState.customFrom,
                    onDateSelected = { date ->
                        viewModel.onEvent(ReportUiEvent.OnDatePicked(date, isStartDate = true))
                        showFromDatePicker = false
                    },
                    onDismiss = {
                        showFromDatePicker = false
                    }
                )
            }

            // "To" date picker dialog
            if (showToDatePicker) {
                DatePickerDialogWrapper(
                    initialDate = uiState.customTo,
                    onDateSelected = { date ->
                        viewModel.onEvent(ReportUiEvent.OnDatePicked(date, isStartDate = false))
                        showToDatePicker = false
                    },
                    onDismiss = {
                        showToDatePicker = false
                    }
                )
            }
        }
    }
}

/**
 * Main content of the Reports screen: preset chips, date pickers, and search button.
 */
@Composable
private fun BerichteContent(
    state: ReportUiState,
    onPresetSelected: (DateRangePreset) -> Unit,
    onFromDateClicked: () -> Unit,
    onToDateClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValidationError = state.activeError is UiErrorType.Validation

    Column(modifier = modifier) {
        // Screen title
        Text(
            text = stringResource(R.string.reports_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Date range selection component
        DateSelectionSection(
            selectedPreset = state.selectedPreset.toUiPreset(),
            customFrom = state.customFrom,
            customTo = state.customTo,
            hasValidationError = hasValidationError,
            onPresetSelected = { uiPreset ->
                onPresetSelected(uiPreset.toViewModelPreset())
            },
            onFromDateClicked = onFromDateClicked,
            onToDateClicked = onToDateClicked
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Search button
        Button(
            onClick = onSearchClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isSearchEnabled && !state.isLoading
        ) {
            Text(
                text = stringResource(R.string.reports_search),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Wrapper for the Material 3 DatePickerDialog.
 *
 * @param initialDate The date to pre-select (nullable).
 * @param onDateSelected Callback when the user confirms a date selection.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


/**
 * Converts a ViewModel [DateRangePreset] to the UI-layer [UiDateRangePreset].
 */
private fun DateRangePreset.toUiPreset(): UiDateRangePreset {
    return when (this) {
        DateRangePreset.DAY -> UiDateRangePreset.DAY
        DateRangePreset.WEEK -> UiDateRangePreset.WEEK
        DateRangePreset.MONTH -> UiDateRangePreset.MONTH
        DateRangePreset.YEAR -> UiDateRangePreset.YEAR
        DateRangePreset.CUSTOM -> UiDateRangePreset.CUSTOM
    }
}

/**
 * Converts a UI-layer [UiDateRangePreset] back to the ViewModel [DateRangePreset].
 */
private fun UiDateRangePreset.toViewModelPreset(): DateRangePreset {
    return when (this) {
        UiDateRangePreset.DAY -> DateRangePreset.DAY
        UiDateRangePreset.WEEK -> DateRangePreset.WEEK
        UiDateRangePreset.MONTH -> DateRangePreset.MONTH
        UiDateRangePreset.YEAR -> DateRangePreset.YEAR
        UiDateRangePreset.CUSTOM -> DateRangePreset.CUSTOM
    }
}



/**
 * UI-layer date range preset enum.
 *
 * Exists separately from the ViewModel's [DateRangePreset] to keep the
 * screen-layer components independent. Converted via extension functions above.
 */
enum class UiDateRangePreset {
    DAY, WEEK, MONTH, YEAR, CUSTOM
}



@Preview(showBackground = true)
@Composable
private fun BerichteContentPreview() {
    MaterialTheme {
        BerichteContent(
            state = ReportUiState(),
            onPresetSelected = {},
            onFromDateClicked = {},
            onToDateClicked = {},
            onSearchClicked = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BerichteContentCustomPreview() {
    MaterialTheme {
        BerichteContent(
            state = ReportUiState(
                selectedPreset = DateRangePreset.CUSTOM,
                customFrom = LocalDate.of(2026, 1, 1),
                customTo = LocalDate.of(2026, 1, 15)
            ),
            onPresetSelected = {},
            onFromDateClicked = {},
            onToDateClicked = {},
            onSearchClicked = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}