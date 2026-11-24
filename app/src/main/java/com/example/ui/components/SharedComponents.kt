package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PaymentStatus
import com.example.model.RecurrenceType
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerRed
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoTertiary
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.util.DateUtils

@Composable
fun categoryAccentColor(category: WorkCategory): Pair<Color, Color> {
    return when (category) {
        WorkCategory.CLASS_BATCH -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        WorkCategory.OFFICE_WORK -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        WorkCategory.CLIENT_TASK -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer
        WorkCategory.PERSONAL -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
}

fun categoryIcon(category: WorkCategory): ImageVector {
    return when (category) {
        WorkCategory.CLASS_BATCH -> Icons.Rounded.School
        WorkCategory.OFFICE_WORK -> Icons.Rounded.BusinessCenter
        WorkCategory.CLIENT_TASK -> Icons.Rounded.Person
        WorkCategory.PERSONAL -> Icons.Rounded.TaskAlt
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val (fg, bg) = when (status) {
        PaymentStatus.PAID -> SuccessGreen to SuccessContainer
        PaymentStatus.PARTIAL -> IndigoTertiary to IndigoContainer
        PaymentStatus.OVERDUE -> DangerRed to DangerContainer
        PaymentStatus.UNPAID -> WarningAmber to WarningContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkItemCard(
    item: WorkItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onSyncCalendar: () -> Unit,
    onBillClient: () -> Unit,
    onSendTestReminder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (accentColor, containerColor) = categoryAccentColor(item.categoryEnum)
    val cardAlpha = if (item.completed) 0.72f else 1f
    val borderColor by animateColorAsState(
        targetValue = if (item.completed) MaterialTheme.colorScheme.outlineVariant else accentColor.copy(alpha = 0.35f),
        label = "cardBorder"
    )
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onEdit)
            .testTag("work_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_complete_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.completed) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (item.completed) "Mark incomplete" else "Mark complete",
                        tint = if (item.completed) SuccessGreen else accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp, end = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = containerColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIcon(item.categoryEnum),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.categoryEnum.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor
                                )
                            }
                        }

                        if (item.startTime.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${DateUtils.formatTimeDisplay(item.startTime)} · ${DateUtils.formatDuration(item.durationMinutes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.batchOrGroup.isNotBlank() || item.clientName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = listOfNotNull(
                                item.batchOrGroup.takeIf { it.isNotBlank() },
                                item.clientName.takeIf { it.isNotBlank() }?.let { "Client: $it" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Task options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit task") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.calendarSynced) "Re-sync to Calendar" else "Sync to Calendar") },
                            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSyncCalendar()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create client bill") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onBillClient()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Send reminder now") },
                            leadingIcon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSendTestReminder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 10.dp)
            ) {
                if (item.recurrenceEnum != RecurrenceType.NONE) {
                    ActionPill(
                        icon = Icons.Rounded.Repeat,
                        label = item.recurrenceEnum.label,
                        tint = MaterialTheme.colorScheme.tertiary,
                        bg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        onClick = null
                    )
                }

                if (item.reminderMinutesBefore >= 0) {
                    val remLabel = if (item.reminderMinutesBefore == 0L) "Alert at start" else "Alert ${item.reminderMinutesBefore}m prior"
                    ActionPill(
                        icon = Icons.Rounded.NotificationsActive,
                        label = remLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onSendTestReminder
                    )
                }

                ActionPill(
                    icon = if (item.calendarSynced) Icons.Rounded.EventAvailable else Icons.Rounded.CalendarMonth,
                    label = if (item.calendarSynced) "In Calendar" else "Add to Calendar",
                    tint = if (item.calendarSynced) SuccessGreen else MaterialTheme.colorScheme.primary,
                    bg = if (item.calendarSynced) SuccessContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    onClick = onSyncCalendar
                )

                if (item.billable) {
                    val feeLabel = if (item.billingAmount > 0.0) {
                        "Bill ${DateUtils.formatCurrency(item.billingAmount)}"
                    } else {
                        "Create Bill"
                    }
                    ActionPill(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        label = feeLabel,
                        tint = MaterialTheme.colorScheme.secondary,
                        bg = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onBillClient
                    )
                }
            }
        }
    }
}

@Composable
fun ActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: (() -> Unit)?
) {
    Surface(
        color = bg,
        shape = CircleShape,
        modifier = if (onClick != null) {
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
        } else {
            Modifier
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    showHeroImage: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            if (showHeroImage) {
                Image(
                    painter = painterResource(id = R.drawable.img_workspace_hero),
                    contentDescription = "Workspace desk illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}
