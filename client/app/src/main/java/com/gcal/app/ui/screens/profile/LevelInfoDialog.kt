package com.gcal.app.ui.screens.profile

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * LevelInfoDialog — Modal dialog displaying the user's gamification status

 *
 * @param currentLevel The user's current level
 * @param currentXp The user's total experience points.
 * @param xpForNextLevel The XP threshold required to reach the next level.
 * @param progressToNextLevel Float between 0.0 and 1.0 for the progress bar.
 * @param onCloseClicked Callback to dismiss the dialog.
 * @param modifier Modifier for the dialog card.
 */
@Composable
fun LevelInfoDialog(
    currentLevel: Int,
    currentXp: Int,
    xpForNextLevel: Int,
    progressToNextLevel: Float,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onCloseClicked) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with close button
                Box(modifier = Modifier.fillMaxWidth()) {
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

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = stringResource(R.string.level_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Level number — large and prominent
                Text(
                    text = stringResource(R.string.level_current, currentLevel),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Star visualization
                LevelStars(
                    currentLevel = currentLevel,
                    maxLevel = 5,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // XP section label
                Text(
                    text = stringResource(R.string.level_xp_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // XP progress bar and numbers
                XpProgressSection(
                    currentXp = currentXp,
                    xpForNextLevel = xpForNextLevel,
                    progress = progressToNextLevel
                )
            }
        }
    }
}

/**
 * LevelStars — Visual star indicator for the current level.
 *
 * Renders filled stars for achieved levels and outlined stars for remaining ones.
 * Capped at [maxLevel] stars to prevent overflow in the UI.
 */
@Composable
private fun LevelStars(
    currentLevel: Int,
    maxLevel: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        repeat(maxLevel) { index ->
            Icon(
                imageVector = if (index < currentLevel) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (index < currentLevel) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                modifier = Modifier.size(32.dp)
            )
            if (index < maxLevel - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/**
 * XpProgressSection — Displays XP numbers and a progress bar.
 *
 * Shows the current XP on the left, the target XP on the right,
 * a [LinearProgressIndicator] in between, and the percentage below.
 */
@Composable
private fun XpProgressSection(
    currentXp: Int,
    xpForNextLevel: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$currentXp XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$xpForNextLevel XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))


        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = stringResource(R.string.level_progress_percent, (progress * 100).toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}