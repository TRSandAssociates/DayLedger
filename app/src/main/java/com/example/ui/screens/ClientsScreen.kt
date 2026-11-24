package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Client
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.util.DateUtils

@Composable
fun ClientsScreen(
    clientsState: UiState<List<Client>>,
    invoicesState: UiState<List<Invoice>>,
    onAddClient: () -> Unit,
    onEditClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onAddTaskForClient: (Client) -> Unit,
    onCreateInvoiceForClient: (Client) -> Unit,
    onCallClient: (String) -> Unit,
    onEmailClient: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allClients = (clientsState as? UiState.Success)?.data ?: emptyList()
    val allInvoices = (invoicesState as? UiState.Success)?.data ?: emptyList()

    val filteredClients = remember(allClients, searchQuery) {
        if (searchQuery.isBlank()) allClients
        else {
            val q = searchQuery.trim().lowercase()
            allClients.filter {
                it.name.lowercase().contains(q) || it.organization.lowercase().contains(q)
            }
        }
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
                Column {
                    Text(
                        text = "CLIENT & BATCH DIRECTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Clients & Groups",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search clients, schools, or batches") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (clientsState) {
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
                                text = clientsState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (filteredClients.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = if (searchQuery.isBlank()) "No clients or batches yet" else "No matching clients",
                                subtitle = "Add clients, coaching groups, or organizations with default rates for quick task scheduling and billing.",
                                actionLabel = "Add Client",
                                onAction = onAddClient
                            )
                        }
                    } else {
                        items(filteredClients, key = { it.id }) { client ->
                            val clientInvoices = allInvoices.filter {
                                it.clientId == client.id || it.clientName.equals(client.name, ignoreCase = true)
                            }
                            val outstanding = clientInvoices
                                .filter { it.statusEnum != PaymentStatus.PAID }
                                .sumOf { it.balanceRemaining }

                            ClientCard(
                                client = client,
                                invoiceCount = clientInvoices.size,
                                outstandingBalance = outstanding,
                                onEdit = { onEditClient(client) },
                                onDelete = { onDeleteClient(client) },
                                onAddTask = { onAddTaskForClient(client) },
                                onCreateInvoice = { onCreateInvoiceForClient(client) },
                                onCall = { onCallClient(client.phone) },
                                onEmail = { onEmailClient(client.email) }
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
private fun ClientCard(
    client: Client,
    invoiceCount: Int,
    outstandingBalance: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddTask: () -> Unit,
    onCreateInvoice: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("client_card_${client.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = client.name.firstOrNull()?.uppercase() ?: "C",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (client.organization.isNotBlank()) {
                        Text(
                            text = client.organization,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Client options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit client") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        if (client.phone.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Call ${client.phone}") },
                                leadingIcon = { Icon(Icons.Rounded.Call, contentDescription = null) },
                                onClick = { menuExpanded = false; onCall() }
                            )
                        }
                        if (client.email.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Email ${client.email}") },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                onClick = { menuExpanded = false; onEmail() }
                            )
                        }
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Default Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (client.defaultRate > 0) DateUtils.formatCurrency(client.defaultRate, client.currencySymbol) else "—",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily)
                    )
                }
                Column {
                    Text("Pending Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DateUtils.formatCurrency(outstandingBalance, client.currencySymbol),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                        color = if (outstandingBalance > 0) MaterialTheme.colorScheme.secondary else SuccessGreen
                    )
                }
                Column {
                    Text("Invoices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$invoiceCount",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onAddTask,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule Work")
                }
                OutlinedButton(
                    onClick = onCreateInvoice,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bill Client")
                }
            }
        }
    }
}
