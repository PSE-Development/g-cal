package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.UiViewMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CalendarHeader - Date header for the calendar
 */
@Composable
fun CalendarHeader(
    currentDate: LocalDate,
    currentMonth: YearMonth,
    currentYear: Int,
    viewMode: UiViewMode,
    modifier: Modifier = Modifier
) {
    val displayText = when (viewMode) {
        UiViewMode.DAY -> {

            val formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.getDefault())
            currentDate.format(formatter)
        }
        UiViewMode.WEEK, UiViewMode.MONTH -> {

            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            currentMonth.atDay(1).format(formatter)
        }
        UiViewMode.YEAR -> {
            currentYear.toString()
        }
    }

    Text(
        text = displayText,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}


@Preview(showBackground = true)
@Composable
private fun CalendarHeaderDayPreview() {
    MaterialTheme {
        CalendarHeader(
            currentDate = LocalDate.of(2026, 1, 18),
            currentMonth = YearMonth.of(2026, 1),
            currentYear = 2026,
            viewMode = UiViewMode.DAY
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarHeaderMonthPreview() {
    MaterialTheme {
        CalendarHeader(
            currentDate = LocalDate.of(2026, 1, 18),
            currentMonth = YearMonth.of(2026, 1),
            currentYear = 2026,
            viewMode = UiViewMode.MONTH
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarHeaderYearPreview() {
    MaterialTheme {
        CalendarHeader(
            currentDate = LocalDate.of(2026, 1, 18),
            currentMonth = YearMonth.of(2026, 1),
            currentYear = 2026,
            viewMode = UiViewMode.YEAR
        )
    }
}