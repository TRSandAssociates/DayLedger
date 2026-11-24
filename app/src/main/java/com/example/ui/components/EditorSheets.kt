package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Client
import com.example.model.DeviceCalendarInfo
import com.example.model.Invoice
import com.example.model.RecurrenceType
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkItemEditorSheet(
    initialItem: WorkItem?,
    defaultDate: String,
    clients: List<Client>,
    hasCalendarPermission: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorkItem, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var category by remember { mutableStateOf(initialItem?.categoryEnum ?: WorkCategory.CLASS_BATCH) }
    var scheduledDate by remember { mutableStateOf(initialItem?.scheduledDate?.ifBlank { defaultDate } ?: defaultDate) }
    var startTime by remember { mutableStateOf(initialItem?.startTime?.ifBlank { "09:00" } ?: "09:00") }
    var hasTime by remember { mutableStateOf(initialItem == null || initialItem.startTime.isNotBlank()) }
    var durationMinutes by remember { mutableStateOf(initialItem?.durationMinutes ?: 60L) }
    var recurrence by remember { mutableStateOf(initialItem?.recurrenceEnum ?: RecurrenceType.NONE) }
    var batchOrGroup by remember { mutableStateOf(initialItem?.batchOrGroup ?: "") }
    var selectedClientId by remember { mutableStateOf(initialItem?.clientId ?: "") }
    var clientName by remember { mutableStateOf(initialItem?.clientName ?: "") }
    var billable by remember { mutableStateOf(initialItem?.billable ?: false) }
    var billingAmountText by remember {
        mutableStateOf(
            if ((initialItem?.billingAmount ?: 0.0) > 0.0) initialItem?.billingAmount.toString() else ""
        )
    }
    var reminderMinutes by remember { mutableStateOf(initialItem?.reminderMinutesBefore ?: 15L) }
    var syncToCalendar by remember { mutableStateOf(initialItem?.calendarSynced ?: hasCalendarPermission) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var clientDropdownOpen by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialItem == null || initialItem.id.isBlank()) "Schedule Task or Batch" else "Edit Task / Batch",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Work Stream",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WorkCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            category = cat
                            if (cat == WorkCategory.CLIENT_TASK && !billable) {
                                billable = true
                            }
                        },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = {
                    Text(
                        when (category) {
                            WorkCategory.CLASS_BATCH -> "Class / Batch Name (e.g. Python Batch A)"
                            WorkCategory.OFFICE_WORK -> "Office Work Title"
                            WorkCategory.CLIENT_TASK -> "Client Deliverable Title"
                            WorkCategory.PERSONAL -> "Daily Task Title"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = batchOrGroup,
                onValueChange = { batchOrGroup = it },
                label = { Text("Batch Code / Project Group (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(DateUtils.formatDisplayDate(scheduledDate).substringBefore(" · "))
                }
                OutlinedButton(
                    onClick = {
                        hasTime = true
                        showTimePicker = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasTime) DateUtils.formatTimeDisplay(startTime) else "All Day")
                }
            }

            if (hasTime) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30L, 45L, 60L, 90L, 120L).forEach { mins ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text(DateUtils.formatDuration(mins)) }
                        )
                    }
                    FilterChip(
                        selected = !hasTime,
                        onClick = { hasTime = false },
                        label = { Text("All Day") }
                    )
                }
            }

            Text(
                text = "Repeat Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.entries.forEach { rec ->
                    FilterChip(
                        selected = recurrence == rec,
                        onClick = { recurrence = rec },
                        label = { Text(rec.label) }
                    )
                }
            }

            // Client picker
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        selectedClientId = ""
                    },
                    label = { Text("Client / Student Group (optional)") },
                    trailingIcon = {
                        if (clients.isNotEmpty()) {
                            TextButton(onClick = { clientDropdownOpen = true }) {
                                Text("Pick")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = clientDropdownOpen,
                    onDismissRequest = { clientDropdownOpen = false }
                ) {
                    clients.forEach { client ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (client.organization.isNotBlank()) "${client.name} (${client.organization})"
                                    else client.name
                                )
                            },
                            onClick = {
                                selectedClientId = client.id
                                clientName = client.name
                                if (client.defaultRate > 0.0 && billingAmountText.isBlank()) {
                                    billingAmountText = client.defaultRate.toString()
                                    billable = true
                                }
                                clientDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Billable Work", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Track fee & create client invoice directly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = billable, onCheckedChange = { billable = it })
                    }
                    if (billable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = billingAmountText,
                            onValueChange = { billingAmountText = it },
                            label = { Text("Expected Fee Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Text(
                text = "Reminder Alert",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-1L to "Off", 0L to "At start", 15L to "15m prior", 30L to "30m prior", 60L to "1h prior").forEach { (mins, label) ->
                    FilterChip(
                        selected = reminderMinutes == mins,
                        onClick = { reminderMinutes = mins },
                        label = { Text(label) }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sync to Device Calendar", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (hasCalendarPermission) "Creates or updates event in your Calendar"
                            else "Connect Calendar in Schedule tab for auto-sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = syncToCalendar,
                        enabled = hasCalendarPermission,
                        onCheckedChange = { syncToCalendar = it }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Syllabus / Agenda") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationError = "Please enter a task or batch title."
                        return@Button
                    }
                    val fee = billingAmountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val updated = (initialItem ?: WorkItem()).copy(
                        title = title.trim(),
                        category = category.name,
                        scheduledDate = scheduledDate,
                        startTime = if (hasTime) startTime else "",
                        durationMinutes = durationMinutes,
                        recurrence = recurrence.name,
                        batchOrGroup = batchOrGroup.trim(),
                        clientId = selectedClientId,
                        clientName = clientName.trim(),
                        billable = billable,
                        billingAmount = if (billable) fee else 0.0,
                        reminderMinutesBefore = reminderMinutes,
                        notes = notes.trim()
                    )
                    onSave(updated, syncToCalendar && hasCalendarPermission)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button")
            ) {
                Text(if (initialItem == null || initialItem.id.isBlank()) "Save to Schedule" else "Update Task")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoDateToMillis(scheduledDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            scheduledDate = DateUtils.millisToIsoDate(it)
                        }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val parts = startTime.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startTime = String.format(Locale.US, "%02d:%02d", timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientEditorSheet(
    initialClient: Client?,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var organization by remember { mutableStateOf(initialClient?.organization ?: "") }
    var email by remember { mutableStateOf(initialClient?.email ?: "") }
    var phone by remember { mutableStateOf(initialClient?.phone ?: "") }
    var rateText by remember {
        mutableStateOf(
            if ((initialClient?.defaultRate ?: 0.0) > 0.0) initialClient?.defaultRate.toString() else ""
        )
    }
    var currency by remember { mutableStateOf(initialClient?.currencySymbol ?: "$") }
    var notes by remember { mutableStateOf(initialClient?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initialClient == null || initialClient.id.isBlank()) "Add Client or Batch" else "Edit Client",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMsg = null },
                label = { Text("Client / Student Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("client_name_input")
            )
            OutlinedTextField(
                value = organization,
                onValueChange = { organization = it },
                label = { Text("Company / School / Batch Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Default Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Currency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("$", "₹", "€", "£").forEach { sym ->
                    FilterChip(
                        selected = currency == sym,
                        onClick = { currency = sym },
                        label = { Text(sym) }
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Billing / Schedule Notes") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Client name is required."
                        return@Button
                    }
                    onSave(
                        (initialClient ?: Client()).copy(
                            name = name.trim(),
                            organization = organization.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            defaultRate = rateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                            currencySymbol = currency,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_client_button")
            ) {
                Text("Save Client")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvoiceEditorSheet(
    initialInvoice: Invoice?,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedClientId by remember { mutableStateOf(initialInvoice?.clientId ?: "") }
    var clientName by remember { mutableStateOf(initialInvoice?.clientName ?: "") }
    var workTitle by remember { mutableStateOf(initialInvoice?.workTitle ?: "") }
    var invoiceNumber by remember {
        mutableStateOf(
            initialInvoice?.invoiceNumber?.ifBlank { "INV-${(1000..9999).random()}" }
                ?: "INV-${(1000..9999).random()}"
        )
    }
    var amountText by remember {
        mutableStateOf(if ((initialInvoice?.amount ?: 0.0) > 0.0) initialInvoice?.amount.toString() else "")
    }
    var currency by remember { mutableStateOf(initialInvoice?.currencySymbol ?: "$") }
    var dueDate by remember {
        mutableStateOf(initialInvoice?.dueDate?.ifBlank { DateUtils.offsetDaysIso(DateUtils.todayIso(), 7) }
            ?: DateUtils.offsetDaysIso(DateUtils.todayIso(), 7))
    }
    var reminderEnabled by remember { mutableStateOf(initialInvoice?.reminderEnabled ?: true) }
    var notes by remember { mutableStateOf(initialInvoice?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var clientPickerOpen by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initialInvoice == null || initialInvoice.id.isBlank()) "Create Client Invoice" else "Edit Invoice",
                style = MaterialTheme.typography.headlineSmall
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        selectedClientId = ""
                        errorMsg = null
                    },
                    label = { Text("Client Name *") },
                    trailingIcon = {
                        if (clients.isNotEmpty()) {
                            TextButton(onClick = { clientPickerOpen = true }) { Text("Pick") }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_client_input")
                )
                DropdownMenu(
                    expanded = clientPickerOpen,
                    onDismissRequest = { clientPickerOpen = false }
                ) {
                    clients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text(client.name) },
                            onClick = {
                                selectedClientId = client.id
                                clientName = client.name
                                currency = client.currencySymbol
                                if (amountText.isBlank() && client.defaultRate > 0.0) {
                                    amountText = client.defaultRate.toString()
                                }
                                clientPickerOpen = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = workTitle,
                onValueChange = { workTitle = it; errorMsg = null },
                label = { Text("Work / Batch Sessions Billed *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("invoice_work_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice #") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("invoice_amount_input")
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("$", "₹", "€", "£").forEach { sym ->
                    FilterChip(
                        selected = currency == sym,
                        onClick = { currency = sym },
                        label = { Text(sym) }
                    )
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Due Date: ${DateUtils.formatDisplayDate(dueDate)}")
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payment Due Reminder", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Notify on the due date if unpaid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Payment Notes / Terms") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (clientName.isBlank() || workTitle.isBlank()) {
                        errorMsg = "Client name and work description are required."
                        return@Button
                    }
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid billing amount greater than 0."
                        return@Button
                    }
                    onSave(
                        (initialInvoice ?: Invoice()).copy(
                            invoiceNumber = invoiceNumber.trim(),
                            clientId = selectedClientId,
                            clientName = clientName.trim(),
                            workTitle = workTitle.trim(),
                            amount = amt,
                            currencySymbol = currency,
                            dueDate = dueDate,
                            reminderEnabled = reminderEnabled,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_invoice_button")
            ) {
                Text("Save Invoice")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = DateUtils.isoDateToMillis(dueDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { dueDate = DateUtils.millisToIsoDate(it) }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirmTotalPaid: (Double) -> Unit
) {
    var paymentInput by remember { mutableStateOf(invoice.balanceRemaining.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment · ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Remaining balance: ${DateUtils.formatCurrency(invoice.balanceRemaining, invoice.currencySymbol)} of ${DateUtils.formatCurrency(invoice.amount, invoice.currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = paymentInput,
                    onValueChange = { paymentInput = it; errorMsg = null },
                    label = { Text("Payment Received Now (${invoice.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { paymentInput = (invoice.balanceRemaining / 2.0).toString() },
                        modifier = Modifier.weight(1f)
                    ) { Text("50%") }
                    OutlinedButton(
                        onClick = { paymentInput = invoice.balanceRemaining.toString() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Full Balance") }
                }
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addAmt = paymentInput.toDoubleOrNull()
                    if (addAmt == null || addAmt <= 0.0) {
                        errorMsg = "Enter an amount greater than 0."
                        return@Button
                    }
                    val newTotal = (invoice.paidAmount + addAmt).coerceAtMost(invoice.amount)
                    onConfirmTotalPaid(newTotal)
                }
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CalendarConnectDialog(
    hasCalendarPermission: Boolean,
    hasNotificationPermission: Boolean,
    calendars: List<DeviceCalendarInfo>,
    selectedCalendarId: Long?,
    onRequestCalendarPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectCalendar: (Long) -> Unit,
    onTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calendar & Reminder Connections") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Connect your Android device calendar to view existing events alongside your classes & tasks, and sync schedules directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasCalendarPermission) {
                    Button(
                        onClick = onRequestCalendarPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Calendar Permission")
                    }
                } else if (calendars.isEmpty()) {
                    Text(
                        "Calendar permission granted, but no writable device calendars were found on this device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Default Sync Calendar", style = MaterialTheme.typography.titleSmall)
                    calendars.forEach { cal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCalendar(cal.id) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = cal.id == selectedCalendarId,
                                onClick = { onSelectCalendar(cal.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cal.displayName, style = MaterialTheme.typography.bodyMedium)
                                if (cal.accountName.isNotBlank()) {
                                    Text(
                                        cal.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notifications & Reminders", style = MaterialTheme.typography.titleSmall)
                        if (!hasNotificationPermission) {
                            OutlinedButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Notifications")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reminder notifications enabled", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = onTestNotification,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send Test Reminder")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
