package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.CalendarEvent
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * DayViewContent
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayViewContent(
    currentDate: LocalDate,
    events: List<CalendarEvent>,
    onDateChanged: (LocalDate) -> Unit,
    onEventClicked: (CalendarEvent) -> Unit,
    onEventChecked: (CalendarEvent, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    val middlePage = 10000
    val totalPages = 20000

    val initialPage = remember(currentDate) {
        val daysDiff = (currentDate.toEpochDay() - today.toEpochDay()).toInt()
        (middlePage + daysDiff).coerceIn(0, totalPages - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val daysDiff = pagerState.currentPage - middlePage
            val newDate = today.plusDays(daysDiff.toLong())

            if (newDate != currentDate) {
                onDateChanged(newDate)
            }
        }
    }

    LaunchedEffect(currentDate) {
        val daysDiff = (currentDate.toEpochDay() - today.toEpochDay()).toInt()
        val targetPage = (middlePage + daysDiff).coerceIn(0, totalPages - 1)

        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(targetPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val daysDiff = page - middlePage
        val pageDate = today.plusDays(daysDiff.toLong())

        val dayEvents = remember(events, pageDate) {
            events.filter { it.date == pageDate }.sortedBy { it.startTime ?: "99:99" }
        }

        DayEventList(
            events = dayEvents,
            onEventClicked = onEventClicked,
            onEventChecked = onEventChecked
        )
    }
}

@Composable
private fun DayEventList(
    events: List<CalendarEvent>,
    onEventClicked: (CalendarEvent) -> Unit,
    onEventChecked: (CalendarEvent, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.calendar_no_events),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.calendar_add_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClicked(event) },
                    onCheckClicked = { checked -> onEventChecked(event, checked) },
                    showTime = true,
                    compact = false
                )
            }
        }
    }
}