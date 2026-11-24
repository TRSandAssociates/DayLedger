package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOut
import com.example.calendar.CalendarHelper
import com.example.data.DayLedgerRepository
import com.example.model.Client
import com.example.model.Invoice
import com.example.model.UiState
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.notifications.ReminderScheduler
import com.example.ui.components.CalendarConnectDialog
import com.example.ui.components.ClientEditorSheet
import com.example.ui.components.InvoiceEditorSheet
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.WorkItemEditorSheet
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.viewmodel.DayLedgerViewModel
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

sealed class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Today : AppDestination("today", "Today", Icons.Rounded.Today, Icons.Outlined.Today)
    data object Schedule : AppDestination("schedule", "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Billing : AppDestination("billing", "Billing", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)
    data object Clients : AppDestination("clients", "Clients", Icons.Rounded.Group, Icons.Outlined.Group)

    companion object {
        val all = listOf(Today, Schedule, Billing, Clients)
    }
}

@Composable
fun DayLedgerApp(
    currentUserId: String,
    userDisplayName: String,
    userPhotoUrl: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DayLedgerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DayLedgerViewModel(DayLedgerRepository(db), currentUserId)
            }
        }
    )

    val workItemsState by viewModel.workItemsState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsState.collectAsStateWithLifecycle()
    val invoicesState by viewModel.invoicesState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedBillingFilter by viewModel.selectedBillingFilter.collectAsStateWithLifecycle()
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsStateWithLifecycle()
    val deviceCalendars by viewModel.deviceCalendars.collectAsStateWithLifecycle()
    val selectedCalendarId by viewModel.selectedCalendarId.collectAsStateWithLifecycle()
    val deviceEvents by viewModel.deviceEvents.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.Today.route

    var hasNotifPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }

    var editingWorkItem by remember { mutableStateOf<WorkItem?>(null) }
    var showWorkEditor by remember { mutableStateOf(false) }

    var editingClient by remember { mutableStateOf<Client?>(null) }
    var showClientEditor by remember { mutableStateOf(false) }

    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showInvoiceEditor by remember { mutableStateOf(false) }

    var paymentTargetInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.all { it }
        viewModel.refreshCalendarState(context)
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Device Calendar connected!"
                else "Calendar permission declined — you can still use Add to Calendar."
            )
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Reminder notifications enabled!"
                else "Notifications disabled — enable in Android Settings anytime."
            )
        }
    }

    LaunchedEffect(selectedDate) {
        viewModel.refreshCalendarState(context)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val clientsList = (clientsState as? UiState.Success)?.data ?: emptyList()

    fun openNewTask(defaultCategory: WorkCategory? = null, client: Client? = null, dateIso: String = selectedDate) {
        editingWorkItem = WorkItem(
            category = (defaultCategory ?: WorkCategory.CLASS_BATCH).name,
            scheduledDate = dateIso,
            clientId = client?.id ?: "",
            clientName = client?.name ?: "",
            billable = client != null,
            billingAmount = client?.defaultRate ?: 0.0
        )
        showWorkEditor = true
    }

    fun openNewInvoice(client: Client? = null, fromTask: WorkItem? = null) {
        editingInvoice = Invoice(
            clientId = fromTask?.clientId ?: client?.id ?: "",
            clientName = fromTask?.clientName?.ifBlank { fromTask.batchOrGroup } ?: client?.name ?: "",
            workTitle = fromTask?.title ?: "",
            workItemId = fromTask?.id ?: "",
            amount = fromTask?.billingAmount?.takeIf { it > 0.0 } ?: client?.defaultRate ?: 0.0,
            currencySymbol = client?.currencySymbol ?: "$"
        )
        showInvoiceEditor = true
    }

    fun syncOrOpenCalendarIntent(item: WorkItem) {
        if (CalendarHelper.hasCalendarPermissions(context)) {
            viewModel.syncExistingTaskToCalendar(context, item)
        } else {
            val intent = CalendarHelper.buildInsertCalendarIntent(item)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
            }
        }
    }

    fun sharePaymentReminder(invoice: Invoice) {
        val text = buildString {
            append("Hi ${invoice.clientName},\n\n")
            append("Friendly reminder for Invoice ${invoice.invoiceNumber} (${invoice.workTitle}).\n")
            append("Balance due: ${DateUtils.formatCurrency(invoice.balanceRemaining, invoice.currencySymbol)} ")
            append("by ${DateUtils.formatDisplayDate(invoice.dueDate)}.\n\n")
            append("Thank you!")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder · ${invoice.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder"))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AppDestination.all.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (currentRoute) {
                    AppDestination.Today.route, AppDestination.Schedule.route -> {
                        ExtendedFloatingActionButton(
                            onClick = { openNewTask() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("Add Task") },
                            modifier = Modifier.testTag("fab_add_task")
                        )
                    }
                    AppDestination.Billing.route -> {
                        ExtendedFloatingActionButton(
                            onClick = { openNewInvoice() },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("New Invoice") },
                            modifier = Modifier.testTag("fab_add_invoice")
                        )
                    }
                    AppDestination.Clients.route -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingClient = null
                                showClientEditor = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("New Client") },
                            modifier = Modifier.testTag("fab_add_client")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AppDestination.all.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Today.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppDestination.Today.route) {
                        TodayScreen(
                            userDisplayName = userDisplayName,
                            userPhotoUrl = userPhotoUrl,
                            selectedDate = selectedDate,
                            selectedCategory = selectedCategory,
                            workItemsState = workItemsState,
                            invoicesState = invoicesState,
                            hasCalendarPermission = hasCalendarPermission,
                            onSelectDate = viewModel::selectDate,
                            onSelectCategory = viewModel::setCategoryFilter,
                            onToggleComplete = { item -> viewModel.toggleWorkItemCompleted(context, item) },
                            onEditTask = { item ->
                                editingWorkItem = item
                                showWorkEditor = true
                            },
                            onAddTask = { openNewTask() },
                            onSyncTaskCalendar = ::syncOrOpenCalendarIntent,
                            onBillTask = { item -> openNewInvoice(fromTask = item) },
                            onSendTestReminder = { item ->
                                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    ReminderScheduler.showImmediateReminder(
                                        context = context,
                                        title = "${item.categoryEnum.label}: ${item.title}",
                                        message = "Scheduled for ${DateUtils.formatDisplayDate(item.scheduledDate)} ${DateUtils.formatTimeDisplay(item.startTime)}",
                                        isPayment = false
                                    )
                                    scope.launch { snackbarHostState.showSnackbar("Reminder notification sent") }
                                }
                            },
                            onDeleteTask = { item -> viewModel.deleteWorkItem(context, item) },
                            onOpenCalendarSettings = { showCalendarDialog = true },
                            onOpenBilling = {
                                navController.navigate(AppDestination.Billing.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSignOut = { signOut(credentialManager, scope) }
                        )
                    }

                    composable(AppDestination.Schedule.route) {
                        ScheduleScreen(
                            selectedDate = selectedDate,
                            workItemsState = workItemsState,
                            hasCalendarPermission = hasCalendarPermission,
                            deviceCalendars = deviceCalendars,
                            selectedCalendarId = selectedCalendarId,
                            deviceEvents = deviceEvents,
                            onSelectDate = viewModel::selectDate,
                            onConnectCalendarClick = {
                                if (!hasCalendarPermission) {
                                    calendarPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                                    )
                                } else {
                                    showCalendarDialog = true
                                }
                            },
                            onRefreshCalendar = { viewModel.refreshCalendarState(context) },
                            onToggleComplete = { item -> viewModel.toggleWorkItemCompleted(context, item) },
                            onEditTask = { item ->
                                editingWorkItem = item
                                showWorkEditor = true
                            },
                            onAddTaskForDate = { dateIso -> openNewTask(dateIso = dateIso) },
                            onSyncTaskCalendar = ::syncOrOpenCalendarIntent,
                            onBillTask = { item -> openNewInvoice(fromTask = item) },
                            onSendTestReminder = { item ->
                                ReminderScheduler.showImmediateReminder(
                                    context = context,
                                    title = "${item.categoryEnum.label}: ${item.title}",
                                    message = "Starts at ${DateUtils.formatTimeDisplay(item.startTime)}",
                                    isPayment = false
                                )
                            },
                            onDeleteTask = { item -> viewModel.deleteWorkItem(context, item) },
                            onImportCalendarEvent = { event, cat ->
                                viewModel.importCalendarEventAsTask(context, event, cat)
                            }
                        )
                    }

                    composable(AppDestination.Billing.route) {
                        BillingScreen(
                            invoicesState = invoicesState,
                            selectedFilter = selectedBillingFilter,
                            onSelectFilter = viewModel::setBillingFilter,
                            onCreateInvoice = { openNewInvoice() },
                            onEditInvoice = { inv ->
                                editingInvoice = inv
                                showInvoiceEditor = true
                            },
                            onRecordPayment = { inv -> paymentTargetInvoice = inv },
                            onMarkPaidFull = { inv ->
                                viewModel.recordInvoicePayment(context, inv, inv.amount)
                            },
                            onShareReminder = { inv -> sharePaymentReminder(inv) },
                            onAddDueToCalendar = { inv ->
                                try {
                                    context.startActivity(CalendarHelper.buildInvoiceDueCalendarIntent(inv))
                                } catch (_: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("No calendar app found") }
                                }
                            },
                            onToggleReminder = { inv -> viewModel.toggleInvoiceReminder(context, inv) },
                            onMarkOverdue = { inv -> viewModel.markInvoiceOverdue(inv) },
                            onDeleteInvoice = { inv -> viewModel.deleteInvoice(context, inv) }
                        )
                    }

                    composable(AppDestination.Clients.route) {
                        ClientsScreen(
                            clientsState = clientsState,
                            invoicesState = invoicesState,
                            onAddClient = {
                                editingClient = null
                                showClientEditor = true
                            },
                            onEditClient = { client ->
                                editingClient = client
                                showClientEditor = true
                            },
                            onDeleteClient = { client -> viewModel.deleteClient(client.id) },
                            onAddTaskForClient = { client ->
                                openNewTask(defaultCategory = WorkCategory.CLIENT_TASK, client = client)
                            },
                            onCreateInvoiceForClient = { client -> openNewInvoice(client = client) },
                            onCallClient = { phone ->
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(dialIntent)
                            },
                            onEmailClient = { email ->
                                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                context.startActivity(emailIntent)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showWorkEditor) {
        WorkItemEditorSheet(
            initialItem = editingWorkItem,
            defaultDate = selectedDate,
            clients = clientsList,
            hasCalendarPermission = hasCalendarPermission,
            onDismiss = { showWorkEditor = false },
            onSave = { item, syncCal ->
                showWorkEditor = false
                viewModel.saveWorkItem(context, item, syncCal)
                if (!hasNotifPermission && item.reminderMinutesBefore >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    if (showClientEditor) {
        ClientEditorSheet(
            initialClient = editingClient,
            onDismiss = { showClientEditor = false },
            onSave = { client ->
                showClientEditor = false
                viewModel.saveClient(client)
            }
        )
    }

    if (showInvoiceEditor) {
        InvoiceEditorSheet(
            initialInvoice = editingInvoice,
            clients = clientsList,
            onDismiss = { showInvoiceEditor = false },
            onSave = { invoice ->
                showInvoiceEditor = false
                viewModel.saveInvoice(context, invoice)
                if (!hasNotifPermission && invoice.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    paymentTargetInvoice?.let { target ->
        RecordPaymentDialog(
            invoice = target,
            onDismiss = { paymentTargetInvoice = null },
            onConfirmTotalPaid = { newTotal ->
                paymentTargetInvoice = null
                viewModel.recordInvoicePayment(context, target, newTotal)
            }
        )
    }

    if (showCalendarDialog) {
        CalendarConnectDialog(
            hasCalendarPermission = hasCalendarPermission,
            hasNotificationPermission = hasNotifPermission,
            calendars = deviceCalendars,
            selectedCalendarId = selectedCalendarId,
            onRequestCalendarPermission = {
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
            },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onSelectCalendar = { calId -> viewModel.selectDeviceCalendar(calId) },
            onTestNotification = {
                ReminderScheduler.showImmediateReminder(
                    context = context,
                    title = "DayLedger Reminder Active",
                    message = "You'll receive timely alerts for class batches, client tasks, and invoice due dates.",
                    isPayment = false
                )
            },
            onDismiss = { showCalendarDialog = false }
        )
    }
}
