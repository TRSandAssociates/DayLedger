package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DeviceCalendarEvent
import com.example.model.DeviceCalendarInfo
import com.example.model.UiState
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WorkItemCard
import com.example.util.DateUtils

@Composable
fun ScheduleScreen(
    selectedDate: String,
    workItemsState: UiState<List<WorkItem>>,
    hasCalendarPermission: Boolean,
    deviceCalendars: List<DeviceCalendarInfo>,
    selectedCalendarId: Long?,
    deviceEvents: List<DeviceCalendarEvent>,
    onSelectDate: (String) -> Unit,
    onConnectCalendarClick: () -> Unit,
    onRefreshCalendar: () -> Unit,
    onToggleComplete: (WorkItem) -> Unit,
    onEditTask: (WorkItem) -> Unit,
    onAddTaskForDate: (String) -> Unit,
    onSyncTaskCalendar: (WorkItem) -> Unit,
    onBillTask: (WorkItem) -> Unit,
    onSendTestReminder: (WorkItem) -> Unit,
    onDeleteTask: (WorkItem) -> Unit,
    onImportCalendarEvent: (DeviceCalendarEvent, WorkCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember(selectedDate) { DateUtils.weekDaysAround(selectedDate) }
    val allItems = (workItemsState as? UiState.Success)?.data ?: emptyList()

    val dayTasks = remember(allItems, selectedDate) {
        allItems.filter {
            DateUtils.doesRecurrenceApplyOnDate(it.scheduledDate, it.recurrence, selectedDate)
        }.sortedBy { if (it.startTime.isBlank()) "99:99" else it.startTime }
    }

    val dayDeviceEvents = remember(deviceEvents, selectedDate) {
        val (dayStart, dayEnd) = DateUtils.dayRangeMillis(selectedDate)
        deviceEvents.filter { ev ->
            ev.startMillis <= dayEnd && ev.endMillis >= dayStart
        }
    }

    val activeCalendar = remember(deviceCalendars, selectedCalendarId) {
        deviceCalendars.firstOrNull { it.id == selectedCalendarId } ?: deviceCalendars.firstOrNull()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "CALENDAR & BATCH SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = DateUtils.formatMonthYear(selectedDate),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { onSelectDate(DateUtils.offsetDaysIso(selectedDate, -7)) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week")
                        }
                        IconButton(
                            onClick = { onSelectDate(DateUtils.offsetDaysIso(selectedDate, 7)) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week")
                        }
                    }
                }
            }

            // Device Calendar Connection Banner
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCalendarPermission) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (hasCalendarPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasCalendarPermission) Icons.Rounded.EventAvailable else Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (hasCalendarPermission) {
                                        activeCalendar?.displayName ?: "Device Calendar Connected"
                                    } else {
                                        "Connect Your Android Calendar"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (hasCalendarPermission) {
                                        "${dayDeviceEvents.size} calendar event(s) on this day"
                                    } else {
                                        "View calendar conflicts & sync batches automatically"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasCalendarPermission) {
                            Row {
                                IconButton(
                                    onClick = onRefreshCalendar,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh calendar events")
                                }
                                TextButton(onClick = onConnectCalendarClick) {
                                    Text("Switch")
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = onConnectCalendarClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }

            // 7-day interactive strip
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        weekDays.forEach { dayIso ->
                            val isSelected = dayIso == selectedDate
                            val isToday = dayIso == DateUtils.todayIso()
                            val hasTasks = allItems.any {
                                DateUtils.doesRecurrenceApplyOnDate(it.scheduledDate, it.recurrence, dayIso)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable { onSelectDate(dayIso) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = DateUtils.formatDayOfWeekShort(dayIso),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DateUtils.formatDayNumber(dayIso),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasTasks && isSelected -> MaterialTheme.colorScheme.onPrimary
                                                hasTasks -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Section: Scheduled DayLedger Tasks & Batches
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scheduled Work (${dayTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { onAddTaskForDate(selectedDate) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule")
                    }
                }
            }

            if (dayTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Nothing scheduled on ${DateUtils.formatDisplayDate(selectedDate)}",
                        subtitle = "Schedule a class batch, office block, or client session for this date.",
                        actionLabel = "Schedule on This Day",
                        onAction = { onAddTaskForDate(selectedDate) }
                    )
                }
            } else {
                items(dayTasks, key = { "sched_${it.id}" }) { item ->
                    WorkItemCard(
                        item = item,
                        onToggleComplete = { onToggleComplete(item) },
                        onEdit = { onEditTask(item) },
                        onSyncCalendar = { onSyncTaskCalendar(item) },
                        onBillClient = { onBillTask(item) },
                        onSendTestReminder = { onSendTestReminder(item) },
                        onDelete = { onDeleteTask(item) }
                    )
                }
            }

            // Section: Device Calendar Events (for importing / avoiding conflicts)
            if (hasCalendarPermission) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "From Your Device Calendar (${dayDeviceEvents.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (dayDeviceEvents.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No external calendar events found on ${DateUtils.formatDisplayDate(selectedDate)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(dayDeviceEvents, key = { "ev_${it.id}_${it.startMillis}" }) { event ->
                        DeviceCalendarEventCard(
                            event = event,
                            alreadyImported = dayTasks.any { it.calendarEventId == event.id },
                            onImportAsTask = { onImportCalendarEvent(event, WorkCategory.CLIENT_TASK) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCalendarEventCard(
    event: DeviceCalendarEvent,
    alreadyImported: Boolean,
    onImportAsTask: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (event.allDay) "All Day" else "${DateUtils.formatMillisTime(event.startMillis)} – ${DateUtils.formatMillisTime(event.endMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (alreadyImported) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Synced",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onImportAsTask,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import")
                }
            }
        }
    }
}
