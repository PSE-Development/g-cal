package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.CalendarEvent
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * YearViewContent

 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YearViewContent(
    currentYear: Int,
    currentMonth: YearMonth,
    events: List<CalendarEvent>,
    onYearChanged: (Int) -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {

    val thisYear = remember { LocalDate.now().year }
    val middlePage = 50
    val totalPages = 101

    val initialPage = remember(currentYear) {
        val yearDiff = currentYear - thisYear
        (middlePage + yearDiff).coerceIn(0, totalPages - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )


    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val yearDiff = pagerState.currentPage - middlePage
            val newYear = thisYear + yearDiff

            if (newYear != currentYear) {
                onYearChanged(newYear)
            }
        }
    }


    LaunchedEffect(currentYear) {
        val yearDiff = currentYear - thisYear
        val targetPage = (middlePage + yearDiff).coerceIn(0, totalPages - 1)

        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val yearDiff = page - middlePage
        val pageYear = thisYear + yearDiff

        YearGrid(
            year = pageYear,
            selectedMonth = currentMonth,
            events = events,
            onMonthSelected = { month ->
                onMonthSelected(YearMonth.of(pageYear, month))
            }
        )
    }
}

@Composable
private fun YearGrid(
    year: Int,
    selectedMonth: YearMonth,
    events: List<CalendarEvent>,
    onMonthSelected: (Month) -> Unit,
    modifier: Modifier = Modifier
) {
    val realNow = YearMonth.now()


    val eventCountByMonth = remember(events, year) {
        events
            .filter { it.date.year == year }
            .groupBy { it.date.month }
            .mapValues { it.value.size }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Month.entries.toList()) { month ->
            val thisMonth = YearMonth.of(year, month)
            val isSelected = thisMonth == selectedMonth
            val isRealToday = thisMonth == realNow
            val eventCount = eventCountByMonth[month] ?: 0

            MonthCard(
                month = month,
                isSelected = isSelected,
                isRealToday = isRealToday,
                eventCount = eventCount,
                onClick = { onMonthSelected(month) }
            )
        }
    }
}

@Composable
private fun MonthCard(
    month: Month,
    isSelected: Boolean,
    isRealToday: Boolean,
    eventCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .then(

                if (isRealToday && !isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                fontSize = 16.sp,
                fontWeight = if (isSelected || isRealToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isRealToday -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )


            if (eventCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.calendar_event_count, eventCount),
                    fontSize = 11.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}