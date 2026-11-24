package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.UiState
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.WorkItemCard
import com.example.ui.components.categoryIcon
import com.example.util.DateUtils

@Composable
fun TodayScreen(
    userDisplayName: String,
    userPhotoUrl: String?,
    selectedDate: String,
    selectedCategory: WorkCategory?,
    workItemsState: UiState<List<WorkItem>>,
    invoicesState: UiState<List<Invoice>>,
    hasCalendarPermission: Boolean,
    onSelectDate: (String) -> Unit,
    onSelectCategory: (WorkCategory?) -> Unit,
    onToggleComplete: (WorkItem) -> Unit,
    onEditTask: (WorkItem) -> Unit,
    onAddTask: () -> Unit,
    onSyncTaskCalendar: (WorkItem) -> Unit,
    onBillTask: (WorkItem) -> Unit,
    onSendTestReminder: (WorkItem) -> Unit,
    onDeleteTask: (WorkItem) -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onOpenBilling: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }

    val allItems = (workItemsState as? UiState.Success)?.data ?: emptyList()
    val dayItems = remember(allItems, selectedDate) {
        allItems.filter { item ->
            DateUtils.doesRecurrenceApplyOnDate(
                scheduledDate = item.scheduledDate,
                recurrenceRaw = item.recurrence,
                targetDate = selectedDate
            )
        }
    }
    val filteredDayItems = remember(dayItems, selectedCategory) {
        if (selectedCategory == null) dayItems else dayItems.filter { it.categoryEnum == selectedCategory }
    }

    val completedCount = dayItems.count { it.completed }
    val totalCount = dayItems.size
    val classBatchCount = dayItems.count { it.categoryEnum == WorkCategory.CLASS_BATCH }
    val allInvoices = (invoicesState as? UiState.Success)?.data ?: emptyList()
    val pendingInvoicesCount = allInvoices.count { it.statusEnum != PaymentStatus.PAID }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top editorial header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAYLEDGER DESK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hi, ${userDisplayName.substringBefore(" ").ifBlank { "There" }}",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onOpenCalendarSettings,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Calendar and reminder settings",
                                tint = if (hasCalendarPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable { profileMenuExpanded = true }
                            ) {
                                if (!userPhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "Account menu",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userDisplayName.firstOrNull()?.uppercase() ?: "U",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = { profileMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Calendar & Reminders") },
                                    leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onOpenCalendarSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign out") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onSignOut()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Daily overview hero card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Daily Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = if (totalCount == 0) "No tasks scheduled yet" else "$completedCount of $totalCount completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = if (totalCount == 0) "0%" else "${(completedCount * 100 / totalCount)}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat() },
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SummaryMetricPill(
                                icon = Icons.Rounded.School,
                                value = "$classBatchCount Batches",
                                onClick = { onSelectCategory(WorkCategory.CLASS_BATCH) },
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricPill(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                value = "$pendingInvoicesCount Unpaid",
                                onClick = onOpenBilling,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Date selector bar
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { onSelectDate(DateUtils.offsetDaysIso(selectedDate, -1)) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DateUtils.formatDisplayDate(selectedDate),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selectedDate != DateUtils.todayIso()) {
                                TextButton(
                                    onClick = { onSelectDate(DateUtils.todayIso()) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Jump to Today", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        IconButton(
                            onClick = { onSelectDate(DateUtils.offsetDaysIso(selectedDate, 1)) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                        }
                    }
                }
            }

            // Category filter chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All (${dayItems.size})") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    WorkCategory.entries.forEach { cat ->
                        val count = dayItems.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                            label = { Text("${cat.shortLabel} ($count)") },
                            leadingIcon = {
                                Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            when (workItemsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(40.dp)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = workItemsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredDayItems.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = if (selectedCategory == null) "Clear schedule for this day" else "No ${selectedCategory.label.lowercase()}s today",
                                subtitle = "Add a class batch, office task, or billable client deliverable and sync it directly with your Calendar.",
                                actionLabel = "Add Task or Batch",
                                onAction = onAddTask,
                                showHeroImage = true
                            )
                        }
                    } else {
                        items(filteredDayItems, key = { it.id }) { item ->
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
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
