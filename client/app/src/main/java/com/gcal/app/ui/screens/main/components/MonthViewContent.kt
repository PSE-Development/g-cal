package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.CalendarEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * MonthViewContent
 */
@Composable
fun MonthViewContent(
    currentMonth: YearMonth,
    events: List<CalendarEvent>,
    onMonthChanged: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val coroutineScope = rememberCoroutineScope()


    val startMonth = remember { YearMonth.now().minusMonths(100) }
    val endMonth = remember { YearMonth.now().plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    var isScrollingProgrammatically by remember { mutableStateOf(false) }

    LaunchedEffect(calendarState.firstVisibleMonth.yearMonth) {
        val visibleMonth = calendarState.firstVisibleMonth.yearMonth

        if (!isScrollingProgrammatically && visibleMonth != currentMonth) {
            onMonthChanged(visibleMonth)
        }
    }

    LaunchedEffect(currentMonth) {
        if (calendarState.firstVisibleMonth.yearMonth != currentMonth) {
            isScrollingProgrammatically = true
            coroutineScope.launch {
                calendarState.animateScrollToMonth(currentMonth)
                isScrollingProgrammatically = false
            }
        }
    }

    val eventsByDate = remember(events) {
        events.groupBy { it.date }
    }

    Column(modifier = modifier.fillMaxSize()) {
        WeekDaysHeader(daysOfWeek = daysOfWeek)

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                MonthDay(
                    day = day,
                    isToday = day.date == today,
                    events = eventsByDate[day.date] ?: emptyList(),
                    onClick = { onDaySelected(day.date) }
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WeekDaysHeader(
    daysOfWeek: List<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthDay(
    day: CalendarDay,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    !isCurrentMonth -> Color.Transparent
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.error
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (events.isNotEmpty() && isCurrentMonth) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    events.take(3).forEach { event ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(event.groupColor))
                        )
                    }

                    if (events.size > 3) {
                        Text(
                            text = "+",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}