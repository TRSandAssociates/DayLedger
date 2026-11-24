package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.util.DateUtils

@Composable
fun BillingScreen(
    invoicesState: UiState<List<Invoice>>,
    selectedFilter: PaymentStatus?,
    onSelectFilter: (PaymentStatus?) -> Unit,
    onCreateInvoice: () -> Unit,
    onEditInvoice: (Invoice) -> Unit,
    onRecordPayment: (Invoice) -> Unit,
    onMarkPaidFull: (Invoice) -> Unit,
    onShareReminder: (Invoice) -> Unit,
    onAddDueToCalendar: (Invoice) -> Unit,
    onToggleReminder: (Invoice) -> Unit,
    onMarkOverdue: (Invoice) -> Unit,
    onDeleteInvoice: (Invoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val allInvoices = (invoicesState as? UiState.Success)?.data ?: emptyList()
    val filtered = remember(allInvoices, selectedFilter) {
        if (selectedFilter == null) allInvoices else allInvoices.filter { it.statusEnum == selectedFilter }
    }

    val totalBilled = allInvoices.sumOf { it.amount }
    val totalCollected = allInvoices.sumOf { it.paidAmount }
    val totalPending = (totalBilled - totalCollected).coerceAtLeast(0.0)
    val primaryCurrency = allInvoices.firstOrNull()?.currencySymbol ?: "$"

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "BILLING & PAYMENT LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Client Invoices",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Financial Summary Ledger Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Outstanding Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateUtils.formatCurrency(totalPending, primaryCurrency),
                                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = MonoFontFamily),
                                    color = if (totalPending > 0) MaterialTheme.colorScheme.secondary else SuccessGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Collected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateUtils.formatCurrency(totalCollected, primaryCurrency),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                    color = SuccessGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = if (totalBilled <= 0.0) 0f else (totalCollected / totalBilled).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total Billed: ${DateUtils.formatCurrency(totalBilled, primaryCurrency)} across ${allInvoices.size} invoice(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status filter chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text("All (${allInvoices.size})") }
                    )
                    PaymentStatus.entries.forEach { st ->
                        val count = allInvoices.count { it.statusEnum == st }
                        FilterChip(
                            selected = selectedFilter == st,
                            onClick = { onSelectFilter(if (selectedFilter == st) null else st) },
                            label = { Text("${st.label} ($count)") }
                        )
                    }
                }
            }

            when (invoicesState) {
                is UiState.Loading -> {
                    item {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(40.dp)) {
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
                                text = invoicesState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No invoices here yet",
                                subtitle = "Create a client invoice from any billable task or add a new billing entry to track payments and reminders.",
                                actionLabel = "Create Invoice",
                                onAction = onCreateInvoice
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { invoice ->
                            InvoiceCard(
                                invoice = invoice,
                                onEdit = { onEditInvoice(invoice) },
                                onRecordPayment = { onRecordPayment(invoice) },
                                onMarkPaidFull = { onMarkPaidFull(invoice) },
                                onShareReminder = { onShareReminder(invoice) },
                                onAddDueToCalendar = { onAddDueToCalendar(invoice) },
                                onToggleReminder = { onToggleReminder(invoice) },
                                onMarkOverdue = { onMarkOverdue(invoice) },
                                onDelete = { onDeleteInvoice(invoice) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InvoiceCard(
    invoice: Invoice,
    onEdit: () -> Unit,
    onRecordPayment: () -> Unit,
    onMarkPaidFull: () -> Unit,
    onShareReminder: () -> Unit,
    onAddDueToCalendar: () -> Unit,
    onToggleReminder: () -> Unit,
    onMarkOverdue: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = invoice.statusEnum == PaymentStatus.PAID
    val isPastDue = !isPaid && DateUtils.isDateBeforeToday(invoice.dueDate)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isPastDue || invoice.statusEnum == PaymentStatus.OVERDUE) DangerRed.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPaid, onClick = onEdit)
            .testTag("invoice_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PaymentStatusBadge(invoice.statusEnum)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isPaid) {
                        IconButton(
                            onClick = onToggleReminder,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (invoice.reminderEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                contentDescription = if (invoice.reminderEnabled) "Reminder active" else "Reminder paused",
                                tint = if (invoice.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Invoice options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (!isPaid) {
                                DropdownMenuItem(
                                    text = { Text("Edit invoice") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                    onClick = { menuExpanded = false; onEdit() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mark paid in full") },
                                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                                    onClick = { menuExpanded = false; onMarkPaidFull() }
                                )
                                if (invoice.statusEnum != PaymentStatus.OVERDUE) {
                                    DropdownMenuItem(
                                        text = { Text("Mark overdue") },
                                        leadingIcon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
                                        onClick = { menuExpanded = false; onMarkOverdue() }
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = { Text("Add due date to Calendar") },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                                onClick = { menuExpanded = false; onAddDueToCalendar() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = invoice.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = invoice.workTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatCurrency(invoice.amount, invoice.currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (invoice.paidAmount > 0.0 && !isPaid) {
                        Text(
                            text = "Paid ${DateUtils.formatCurrency(invoice.paidAmount, invoice.currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPaid) {
                    "Settled on ${DateUtils.formatDisplayDate(invoice.paidDate.ifBlank { invoice.dueDate })}"
                } else if (isPastDue) {
                    "Past due · Due ${DateUtils.formatDisplayDate(invoice.dueDate)}"
                } else {
                    "Due ${DateUtils.formatDisplayDate(invoice.dueDate)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isPastDue) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRecordPayment,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Payment")
                    }
                    OutlinedButton(
                        onClick = onShareReminder,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remind Client")
                    }
                }
            }
        }
    }
}
