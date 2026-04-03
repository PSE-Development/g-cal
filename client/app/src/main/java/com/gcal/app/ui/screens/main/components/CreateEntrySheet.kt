package com.gcal.app.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gcal.app.ui.screens.main.CalendarEvent
import com.gcal.app.ui.screens.main.CalendarGroup
import com.gcal.app.ui.screens.main.Friend
import com.gcal.app.ui.screens.main.UiEntryType
import com.gcal.app.ui.screens.main.UiRepeatOption
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * CreateEntrySheet - create a new entry sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntrySheet(
    initialDate: LocalDate = LocalDate.now(),
    groups: List<CalendarGroup> = emptyList(),
    friends: List<Friend> = emptyList(),
    onSave: (CalendarEvent) -> Unit,
    onDismiss: () -> Unit
) {

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf<LocalTime?>(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf<LocalTime?>(LocalTime.of(10, 0)) }
    var selectedGroup by remember { mutableStateOf<CalendarGroup?>(null) }
    var location by remember { mutableStateOf("") }
    var repeatOption by remember { mutableStateOf(UiRepeatOption.NONE) }
    var selectedType by remember { mutableStateOf(UiEntryType.TODO) }


    var selectedFriends by remember { mutableStateOf<List<Friend>>(emptyList()) }



    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showFriendPicker by remember { mutableStateOf(false) }


    val dateFormatter = remember { DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN) }


    val isFormValid = name.isNotBlank() && (selectedType != UiEntryType.SHARED_EVENT || selectedFriends.isNotEmpty())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.event_new),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UiEntryType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(stringResource(type.labelResId)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.event_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.event_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )


                OutlinedTextField(
                    value = selectedDate.format(dateFormatter),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.event_date_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                )


                if (selectedType != UiEntryType.TODO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime?.format(timeFormatter) ?: stringResource(R.string.event_start_placeholder),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.event_from_label)) },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartTimePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )

                        OutlinedTextField(
                            value = endTime?.format(timeFormatter) ?: stringResource(R.string.event_end_placeholder),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.event_to_label)) },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                    }
                }




if(selectedType != UiEntryType.TODO) {
    OutlinedTextField(
        value = selectedGroup?.name ?: stringResource(R.string.group_none),
        onValueChange = { },
        label = { Text(stringResource(R.string.event_group_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showGroupPicker = true },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        leadingIcon = {
            selectedGroup?.let { group ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(group.color))
                )
            }
        },
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    )
}




                if (selectedType == UiEntryType.SHARED_EVENT) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.event_share_with),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (selectedFriends.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedFriends.forEach { friend ->
                                AssistChip(
                                    onClick = {
                                        selectedFriends = selectedFriends - friend
                                    },
                                    label = { Text(friend.displayName) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.event_remove),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showFriendPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.friends_add))
                    }


                }

                Spacer(modifier = Modifier.height(16.dp))
            }


            Button(
                onClick = {
                    val newEvent = CalendarEvent(
                        id = 0L,
                        title = name,
                        description = description,
                        date = selectedDate,
                        startTime = startTime?.format(timeFormatter) ?: "",
                        endTime = endTime?.format(timeFormatter) ?: "",
                        groupId = selectedGroup?.id,
                        groupName = selectedGroup?.name ?: "",
                        groupColor = selectedGroup?.color ?: 0,
                        eventType = selectedType,
                        repeatOption = repeatOption,
                        location = location,
                        sharedWith = selectedFriends
                    )
                    onSave(newEvent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.event_save))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime ?: LocalTime.of(9, 0),
            onConfirm = { time ->
                startTime = time
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime ?: (startTime?.plusHours(1) ?: LocalTime.of(10, 0)),
            onConfirm = { time ->
                endTime = time
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            groups = groups,
            selectedGroup = selectedGroup,
            onGroupSelected = { group ->
                selectedGroup = group
                showGroupPicker = false
            },
            onDismiss = { showGroupPicker = false }
        )
    }

    if (showFriendPicker) {
        FriendPickerDialog(
            friends = friends,
            selectedFriends = selectedFriends,
            onFriendsSelected = { newFriends ->
                selectedFriends = newFriends
                showFriendPicker = false
            },
            onDismiss = { showFriendPicker = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.event_pick_time),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        }
                    ) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPickerDialog(
    groups: List<CalendarGroup>,
    selectedGroup: CalendarGroup?,
    onGroupSelected: (CalendarGroup?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.event_pick_group),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )



                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupSelected(group) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroup?.id == group.id,
                            onClick = { onGroupSelected(group) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(group.color))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(group.name)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    }
}

@Composable
private fun FriendPickerDialog(
    friends: List<Friend>,
    selectedFriends: List<Friend>,
    onFriendsSelected: (List<Friend>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(selectedFriends) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.friends_add),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (friends.isEmpty()) {
                    Text(
                        text = stringResource(R.string.friends_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(friends) { friend ->
                            val isSelected = tempSelection.any { it.id == friend.id }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelection = if (isSelected) {
                                            tempSelection.filter { it.id != friend.id }
                                        } else {
                                            tempSelection + friend
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        tempSelection = if (checked) {
                                            tempSelection + friend
                                        } else {
                                            tempSelection.filter { it.id != friend.id }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "@${friend.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onFriendsSelected(tempSelection) }) {
                        Text(stringResource(R.string.dialog_confirm) + " (${tempSelection.size})")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun CreateEntrySheetPreview() {
    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("CreateEntrySheet Preview")
            Text("Preview placeholder")
        }
    }
}