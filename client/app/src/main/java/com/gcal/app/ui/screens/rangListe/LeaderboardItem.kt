package com.gcal.app.ui.screens.rangliste

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi

/**
 * LeaderboardItem - Input for a single leaderboard entry.
 *
 */
@Composable
fun LeaderboardItem(
    entry: LeaderboardEntryUi,
    isCurrentUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentUser) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            RankBadge(
                rank = entry.rank,
                isTopThree = entry.rank <= 3
            )


            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))


                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(entry.starCount) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    repeat(5 - entry.starCount) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }


            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = entry.xpDisplayString,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { entry.nextLevelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * RankBadge - Display a badge with the user's rank.
 *
 */
@Composable
private fun RankBadge(
    rank: Int,
    isTopThree: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        rank == 1 -> MaterialTheme.colorScheme.primary
        rank == 2 -> MaterialTheme.colorScheme.secondary
        rank == 3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isTopThree -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// PREVIEWS

@Preview(showBackground = true)
@Composable
private fun LeaderboardItemFirstPlacePreview() {
    MaterialTheme {
        LeaderboardItem(
            entry = LeaderboardEntryUi(
                rank = 1,
                username = "MaxMustermann",
                level = 5,
                starCount = 5,
                xpDisplayString = "XP: 4850",
                nextLevelProgress = 0.85f
            ),
            isCurrentUser = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardItemCurrentUserPreview() {
    MaterialTheme {
        LeaderboardItem(
            entry = LeaderboardEntryUi(
                rank = 5,
                username = "IchSelbst",
                level = 3,
                starCount = 3,
                xpDisplayString = "XP: 2450",
                nextLevelProgress = 0.45f
            ),
            isCurrentUser = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardItemRegularPreview() {
    MaterialTheme {
        LeaderboardItem(
            entry = LeaderboardEntryUi(
                rank = 8,
                username = "EmmaKoch",
                level = 2,
                starCount = 2,
                xpDisplayString = "XP: 1420",
                nextLevelProgress = 0.42f
            ),
            isCurrentUser = false
        )
    }
}