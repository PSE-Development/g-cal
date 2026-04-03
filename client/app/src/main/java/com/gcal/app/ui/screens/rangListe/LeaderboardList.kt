package com.gcal.app.ui.screens.rangliste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi

/**
 * LeaderboardList — A performant scrollable list of leaderboard entries.
 *
 * Uses [LazyColumn] for lazy rendering so that only the currently visible items
 * are composed. This is critical because the leaderboard can grow to hundreds
 * of entries as the user base scales.
 *
 * @param entries        The pre-sorted, pre-formatted list from the ViewModel.
 * @param userRankIndex  Index of the current user for visual highlighting
 * @param modifier       Modifier for the list container.
 * @param contentPadding Padding applied inside the scrollable area.
 */
@Composable
fun LeaderboardList(
    entries: List<LeaderboardEntryUi>,
    userRankIndex: Int = -1,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.rank }
        ) { index, entry ->
            LeaderboardItem(
                entry = entry,
                isCurrentUser = index == userRankIndex
            )
        }
    }
}

// PREVIEWS

@Preview(showBackground = true)
@Composable
private fun LeaderboardListEmptyPreview() {
    MaterialTheme {
        LeaderboardList(
            entries = emptyList()
        )
    }
}