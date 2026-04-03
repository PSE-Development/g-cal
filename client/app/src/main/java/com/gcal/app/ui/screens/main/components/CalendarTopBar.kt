package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.UiViewMode
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * CalendarTopBar - TopBar for the calendar
 */
@Composable
fun CalendarTopBar(
    activeMode: UiViewMode,
    onViewModeSelected: (UiViewMode) -> Unit,
    onFilterClicked: () -> Unit,
    onJumpToDateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UiViewMode.entries.forEach { mode ->
                    ViewModeButton(
                        mode = mode,
                        isSelected = mode == activeMode,
                        onClick = { onViewModeSelected(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = onFilterClicked) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.cd_filter),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }


                IconButton(onClick = onJumpToDateClicked) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.calendar_pick_date),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * ViewModeButton - Button for the view mode
 */
@Composable
private fun ViewModeButton(
    mode: UiViewMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = stringResource(mode.labelResId),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}



@Preview(showBackground = true)
@Composable
private fun CalendarTopBarPreview() {
    MaterialTheme {
        CalendarTopBar(
            activeMode = UiViewMode.MONTH,
            onViewModeSelected = {},
            onFilterClicked = {},
            onJumpToDateClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarTopBarDaySelectedPreview() {
    MaterialTheme {
        CalendarTopBar(
            activeMode = UiViewMode.DAY,
            onViewModeSelected = {},
            onFilterClicked = {},
            onJumpToDateClicked = {}
        )
    }
}