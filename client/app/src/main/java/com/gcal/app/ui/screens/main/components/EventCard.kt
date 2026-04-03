package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcal.app.ui.screens.main.CalendarEvent

/**
 * Compact card component representing a single calendar entry in day and week views.
 *
 *
 * @param event The [CalendarEvent] data to render.
 * @param onClick Callback invoked when the card is tapped, typically opening the editor dialog.
 * @param onCheckClicked Callback invoked when the completion checkbox is toggled.
 *        Receives the new desired completion state (true = mark complete).
 * @param modifier Optional [Modifier] for the card container.
 * @param showTime Whether to display the start/end time row. Disabled in compact layouts
 *        where vertical space is constrained.
 * @param compact When true, reduces internal padding and font sizes for dense list layouts
 *        such as the week view's per-day event list.
 */
@Composable
fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    onCheckClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
    compact: Boolean = false
) {
    val groupColor = Color(event.groupColor)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (compact) 48.dp else 72.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(groupColor)
            )


            IconButton(
                onClick = { onCheckClicked(!event.isCompleted) },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = if (event.isCompleted) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.Circle
                    },
                    contentDescription = if (event.isCompleted) "Erledigt" else "Nicht erledigt",
                    tint = if (event.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }


            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = if (compact) 8.dp else 12.dp,
                        bottom = if (compact) 8.dp else 12.dp,
                        end = 12.dp
                    )
            ) {

                Text(
                    text = event.title,
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )


                if (showTime && event.startTime != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (event.endTime != null) {
                            "${event.startTime} - ${event.endTime}"
                        } else {
                            event.startTime
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!compact && event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.xpValue > 0) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+${event.xpValue} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}