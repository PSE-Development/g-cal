package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gcal.app.ui.screens.main.CalendarGroup
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * FilterDialog
 */
@Composable
fun FilterDialog(
    groups: List<CalendarGroup>,
    onToggleGroup: (Long) -> Unit,
    onCreateGroup: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ========== HEADER mit X-Button ==========
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calendar_filter),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = stringResource(R.string.filter_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))


                if (groups.isEmpty()) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📁",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.filter_no_groups),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.filter_create_first),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            GroupFilterItem(
                                group = group,
                                onToggle = { onToggleGroup(group.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedButton(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.group_create),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }


    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onCreateGroup = { name, color ->
                onCreateGroup(name, color)
                showCreateGroupDialog = false
            },
            onDismiss = { showCreateGroupDialog = false }
        )
    }
}

/**
 * GroupFilterItem
 */
@Composable
private fun GroupFilterItem(
    group: CalendarGroup,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupColor = Color(group.color)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isVisible) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(groupColor)
                    .then(
                        if (!group.isVisible) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                        } else Modifier
                    )
            ) {
                if (!group.isVisible) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = group.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (group.isVisible) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                modifier = Modifier.weight(1f)
            )


            Icon(
                imageVector = if (group.isVisible) {
                    Icons.Default.Visibility
                } else {
                    Icons.Default.VisibilityOff
                },
                contentDescription = if (group.isVisible) "Sichtbar" else "Ausgeblendet",
                tint = if (group.isVisible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

/**
 * CreateGroupDialog
 */
@Composable
fun CreateGroupDialog(
    onCreateGroup: (name: String, color: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val availableColors = listOf(
        0xFF6200EE, // Purple
        0xFF03DAC5, // Teal
        0xFFFF5722, // Orange
        0xFF4CAF50, // Green
        0xFF2196F3, // Blue
        0xFFE91E63, // Pink
        0xFFFF9800, // Amber
        0xFF9C27B0, // Deep Purple
        0xFF00BCD4, // Cyan
        0xFF795548  // Brown
    )

    var groupName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(availableColors.first()) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.group_new),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))


                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.group_name)) },
                    placeholder = { Text("z.B. Arbeit, Sport, Familie...") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))


                Text(
                    text = stringResource(R.string.group_pick_color),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        availableColors.take(5).forEach { color ->
                            ColorPickerItem(
                                color = color,
                                isSelected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        availableColors.drop(5).forEach { color ->
                            ColorPickerItem(
                                color = color,
                                isSelected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Text(
                    text = stringResource(R.string.group_preview),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = groupName.ifEmpty { "Gruppenname" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (groupName.isEmpty()) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Button(
                    onClick = {
                        if (groupName.isBlank()) {
                            nameError = "Bitte einen Namen eingeben"
                        } else {
                            onCreateGroup(groupName.trim(), selectedColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = groupName.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.event_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * ColorPickerItem
 */
@Composable
private fun ColorPickerItem(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}