package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * WeekViewContent
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekViewContent(
    currentDate: LocalDate,
    events: List<CalendarEvent>,
    onDateChanged: (LocalDate) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onEventClicked: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {

    val today = remember { LocalDate.now() }
    val todayMonday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    val middlePage = 5000
    val totalPages = 10000


    val currentMonday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val initialPage = remember(currentDate) {
        val weeksDiff = ((currentMonday.toEpochDay() - todayMonday.toEpochDay()) / 7).toInt()
        (middlePage + weeksDiff).coerceIn(0, totalPages - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )


    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val weeksDiff = pagerState.currentPage - middlePage
            val newWeekMonday = todayMonday.plusWeeks(weeksDiff.toLong())

            val selectedMonday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            if (newWeekMonday != selectedMonday) {
                onDateChanged(newWeekMonday)
            }
        }
    }

    LaunchedEffect(currentDate) {
        val targetMonday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksDiff = ((targetMonday.toEpochDay() - todayMonday.toEpochDay()) / 7).toInt()
        val targetPage = (middlePage + weeksDiff).coerceIn(0, totalPages - 1)

        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val weeksDiff = page - middlePage
        val weekStart = todayMonday.plusWeeks(weeksDiff.toLong())
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

        WeekContent(
            weekDays = weekDays,
            currentDate = currentDate,
            events = events,
            onDaySelected = onDaySelected,
            onEventClicked = onEventClicked
        )
    }
}

@Composable
private fun WeekContent(
    weekDays: List<LocalDate>,
    currentDate: LocalDate,
    events: List<CalendarEvent>,
    onDaySelected: (LocalDate) -> Unit,
    onEventClicked: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    Row(modifier = modifier.fillMaxSize()) {
        weekDays.forEachIndexed { index, date ->
            val isToday = date == today
            val isSelected = date == currentDate
            val dayEvents = remember(events, date) {
                events.filter { it.date == date }.sortedBy { it.startTime ?: "99:99" }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onDaySelected(date) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                isToday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.error
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(dayEvents, key = { "${date}_${it.id}" }) { event ->
                        WeekEventChip(
                            event = event,
                            onClick = { onEventClicked(event) }
                        )
                    }
                }
            }


            if (index < 6) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun WeekEventChip(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(event.groupColor).copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Text(
            text = event.title,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}