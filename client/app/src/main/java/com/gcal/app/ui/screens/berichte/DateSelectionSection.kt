package com.gcal.app.ui.screens.berichte

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * DateSelectionSection — Time range selection component.
 *
 * Provides two selection methods:
 *  1. Preset chips (Day, Week, Month, Year) in a horizontal row.
 *  2. Custom date range via two clickable date fields ("from" / "to")
 *     that open a Material 3 DatePickerDialog.
 *
 * @param selectedPreset     Currently active preset.
 * @param customFrom         Custom start date (nullable, only used with CUSTOM preset).
 * @param customTo           Custom end date (nullable, only used with CUSTOM preset).
 * @param hasValidationError True when "from" date is after "to" date (red border).
 * @param onPresetSelected   Callback when a preset chip is tapped.
 * @param onFromDateClicked  Callback to open the "from" DatePicker.
 * @param onToDateClicked    Callback to open the "to" DatePicker.
 */
@Composable
fun DateSelectionSection(
    selectedPreset: UiDateRangePreset,
    customFrom: LocalDate?,
    customTo: LocalDate?,
    hasValidationError: Boolean,
    onPresetSelected: (UiDateRangePreset) -> Unit,
    onFromDateClicked: () -> Unit,
    onToDateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Column(modifier = modifier) {

        Text(
            text = stringResource(R.string.reports_select_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                text = stringResource(R.string.reports_day),
                selected = selectedPreset == UiDateRangePreset.DAY,
                onClick = { onPresetSelected(UiDateRangePreset.DAY) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                text = stringResource(R.string.reports_week),
                selected = selectedPreset == UiDateRangePreset.WEEK,
                onClick = { onPresetSelected(UiDateRangePreset.WEEK) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                text = stringResource(R.string.reports_month),
                selected = selectedPreset == UiDateRangePreset.MONTH,
                onClick = { onPresetSelected(UiDateRangePreset.MONTH) },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                text = stringResource(R.string.reports_year),
                selected = selectedPreset == UiDateRangePreset.YEAR,
                onClick = { onPresetSelected(UiDateRangePreset.YEAR) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instruction text for custom range
        Text(
            text = stringResource(R.string.reports_or_custom_self),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom date range input fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "from" date field
            DateField(
                label = stringResource(R.string.reports_from),
                date = customFrom,
                dateFormatter = dateFormatter,
                isError = hasValidationError,
                onClick = onFromDateClicked,
                modifier = Modifier.weight(1f)
            )

            // "to" date field
            DateField(
                label = stringResource(R.string.reports_to),
                date = customTo,
                dateFormatter = dateFormatter,
                isError = hasValidationError,
                onClick = onToDateClicked,
                modifier = Modifier.weight(1f)
            )
        }


        if (hasValidationError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reports_validation_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * PresetChip — A selectable filter chip for time range presets.
 */
@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

/**
 * DateField — A clickable card that displays a date and opens the DatePicker on tap.
 */
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    dateFormatter: DateTimeFormatter,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(4.dp))


            Text(
                text = date?.format(dateFormatter) ?: stringResource(R.string.calendar_pick_date),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (date != null) FontWeight.Medium else FontWeight.Normal,
                color = if (date != null) {
                    if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}