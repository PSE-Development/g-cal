package com.gcal.app.ui.screens.berichte

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gcal.app.R
import com.gcal.app.model.modelFacade.general.Achievement
import com.gcal.app.ui.view_model.reportViewModel.ReportResultUi
import java.time.LocalDateTime

/**
 * ReportResultDialog — Modal overlay displaying the generated report statistics and achievements.
 *
 * Renders three statistic rows (completed events, open events, total XP) followed by
 * an achievements section that shows earned badges in a horizontal scrollable row.
 * If no achievements were earned in the selected time range, a placeholder message is shown.
 *
 * @param data           The pre-computed report statistics from the ViewModel.
 * @param onCloseClicked Callback to close this dialog (fires OnCloseDialogClicked event).
 * @param modifier       Optional modifier for the dialog card container.
 */
@Composable
fun ReportResultDialog(
    data: ReportResultUi,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onCloseClicked) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                DialogHeader(onCloseClicked = onCloseClicked)

                Spacer(modifier = Modifier.height(16.dp))

                // Report title
                Text(
                    text = stringResource(R.string.reports_report),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Completed events count
                StatisticItem(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.reports_completed),
                    value = data.completedCount.toString()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Open events count
                StatisticItem(
                    icon = Icons.Filled.PendingActions,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.reports_open),
                    value = data.openCount.toString()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Total XP earned in the selected time range
                StatisticItem(
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.reports_xp),
                    value = "${data.totalXp} XP"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Achievements section header
                Text(
                    text = stringResource(R.string.reports_achievements),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Achievement badges or empty state placeholder
                if (data.earnedAchievements.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(data.earnedAchievements) { achievement ->
                            AchievementBadge(achievement = achievement)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.reports_no_achievements),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * A circular badge representing a single earned achievement.
 *
 * Displays a trophy icon inside a colored circle, the achievement name below it,
 * and a short description text. Used in the horizontal scrollable achievement row.
 *
 * @param achievement The domain achievement object providing name and description.
 * @param modifier    Optional modifier for the badge container.
 */
@Composable
private fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        // Circular badge icon with border
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = achievement.achievementName(),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Achievement display name
        Text(
            text = achievement.achievementName(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Achievement description
        Text(
            text = achievement.achievementDescription(),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dialog header with a close button aligned to the top-start corner.
 *
 * @param onCloseClicked Callback invoked when the close icon is tapped.
 * @param modifier       Optional modifier for the header container.
 */
@Composable
private fun DialogHeader(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onCloseClicked,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A single statistic row displaying an icon, label, and right-aligned value.
 *
 * Used for the three report metrics: completed count, open count, and total XP.
 *
 * @param icon     Leading icon for the statistic category.
 * @param iconTint Color tint applied to the icon.
 * @param label    Descriptive label text (e.g., "Erledigte Aufgaben").
 * @param value    Formatted value string (e.g., "15" or "1250 XP").
 * @param modifier Optional modifier for the row container.
 */
@Composable
private fun StatisticItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportResultDialogPreview() {
    val mockAchievements = listOf(
        object : Achievement {
            override fun achievementName(): String = "Früher Vogel"
            override fun achievementDescription(): String = "Erledige drei ToDo zwischen fünf und zehn Uhr morgens!"
            override fun isCompleted(): Boolean = true
            override fun completionDate(): LocalDateTime = LocalDateTime.now()
        },
        object : Achievement {
            override fun achievementName(): String = "Nachteule"
            override fun achievementDescription(): String = "Erledige drei ToDo nach null und vor fünf Uhr!"
            override fun isCompleted(): Boolean = true
            override fun completionDate(): LocalDateTime = LocalDateTime.now().minusDays(1)
        }
    )

    MaterialTheme {
        ReportResultDialog(
            data = ReportResultUi(
                completedCount = 15,
                openCount = 3,
                totalXp = 1250,
                earnedAchievements = mockAchievements
            ),
            onCloseClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportResultDialogEmptyPreview() {
    MaterialTheme {
        ReportResultDialog(
            data = ReportResultUi(
                completedCount = 0,
                openCount = 0,
                totalXp = 0,
                earnedAchievements = emptyList()
            ),
            onCloseClicked = {}
        )
    }
}