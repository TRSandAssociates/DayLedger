package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.CalendarHelper
import com.example.data.DayLedgerRepository
import com.example.model.Client
import com.example.model.DeviceCalendarEvent
import com.example.model.DeviceCalendarInfo
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.UiState
import com.example.model.WorkCategory
import com.example.model.WorkItem
import com.example.notifications.ReminderScheduler
import com.example.util.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayLedgerViewModel(
    private val repository: DayLedgerRepository,
    val currentUserId: String
) : ViewModel() {

    val workItemsState: StateFlow<UiState<List<WorkItem>>> = repository.observeWorkItems(currentUserId)
        .map<List<WorkItem>, UiState<List<WorkItem>>> { items ->
            val sorted = items.sortedWith(
                compareBy<WorkItem> { it.completed }
                    .thenBy { it.scheduledDate }
                    .thenBy { if (it.startTime.isBlank()) "99:99" else it.startTime }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing work items", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val clientsState: StateFlow<UiState<List<Client>>> = repository.observeClients(currentUserId)
        .map<List<Client>, UiState<List<Client>>> { clients ->
            UiState.Success(clients.sortedBy { it.name.lowercase() })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing clients", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load clients"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val invoicesState: StateFlow<UiState<List<Invoice>>> = repository.observeInvoices(currentUserId)
        .map<List<Invoice>, UiState<List<Invoice>>> { invoices ->
            val sorted = invoices.sortedWith(
                compareBy<Invoice> { it.statusEnum == PaymentStatus.PAID }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing invoices", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load invoices"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedDate = MutableStateFlow(DateUtils.todayIso())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<WorkCategory?>(null)
    val selectedCategoryFilter: StateFlow<WorkCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedBillingFilter = MutableStateFlow<PaymentStatus?>(null)
    val selectedBillingFilter: StateFlow<PaymentStatus?> = _selectedBillingFilter.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(false)
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _deviceCalendars = MutableStateFlow<List<DeviceCalendarInfo>>(emptyList())
    val deviceCalendars: StateFlow<List<DeviceCalendarInfo>> = _deviceCalendars.asStateFlow()

    private val _selectedCalendarId = MutableStateFlow<Long?>(null)
    val selectedCalendarId: StateFlow<Long?> = _selectedCalendarId.asStateFlow()

    private val _deviceEvents = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
    val deviceEvents: StateFlow<List<DeviceCalendarEvent>> = _deviceEvents.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun selectDate(isoDate: String) {
        _selectedDate.value = isoDate
    }

    fun setCategoryFilter(category: WorkCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setBillingFilter(status: PaymentStatus?) {
        _selectedBillingFilter.value = status
    }

    fun selectDeviceCalendar(calendarId: Long) {
        _selectedCalendarId.value = calendarId
        _messages.tryEmit("Connected to device calendar")
    }

    fun refreshCalendarState(context: Context) {
        val granted = CalendarHelper.hasCalendarPermissions(context)
        _hasCalendarPermission.value = granted
        if (!granted) {
            _deviceCalendars.value = emptyList()
            _deviceEvents.value = emptyList()
            return
        }
        viewModelScope.launch {
            val calendars = CalendarHelper.queryWritableCalendars(context)
            _deviceCalendars.value = calendars
            if (_selectedCalendarId.value == null && calendars.isNotEmpty()) {
                _selectedCalendarId.value = calendars.first().id
            }
            val (startMs, _) = DateUtils.dayRangeMillis(DateUtils.offsetDaysIso(_selectedDate.value, -3))
            val (_, endMs) = DateUtils.dayRangeMillis(DateUtils.offsetDaysIso(_selectedDate.value, 10))
            _deviceEvents.value = CalendarHelper.queryEventsForRange(context, startMs, endMs)
        }
    }

    fun saveWorkItem(
        context: Context,
        item: WorkItem,
        syncToCalendar: Boolean
    ) {
        viewModelScope.launch {
            var itemToSave = item
            var syncedNow = false
            if (syncToCalendar && CalendarHelper.hasCalendarPermissions(context)) {
                val calId = _selectedCalendarId.value
                    ?: CalendarHelper.queryWritableCalendars(context).firstOrNull()?.id
                if (calId != null) {
                    _selectedCalendarId.value = calId
                    val eventId = CalendarHelper.upsertWorkItemToCalendar(context, calId, item)
                    if (eventId != null) {
                        itemToSave = item.copy(calendarEventId = eventId, calendarSynced = true)
                        syncedNow = true
                    }
                }
            }
            val result = repository.saveWorkItem(currentUserId, itemToSave)
            result.onSuccess { savedId ->
                val savedItem = itemToSave.copy(id = savedId)
                ReminderScheduler.scheduleWorkItemReminder(context, savedItem)
                if (syncedNow) refreshCalendarState(context)
                _messages.tryEmit(
                    when {
                        syncedNow -> "Saved and synced to device Calendar"
                        item.id.isBlank() -> "Task added to schedule"
                        else -> "Task updated"
                    }
                )
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save task")
            }
        }
    }

    fun toggleWorkItemCompleted(context: Context, item: WorkItem) {
        viewModelScope.launch {
            val next = !item.completed
            repository.toggleWorkItemCompleted(currentUserId, item.id, next)
                .onSuccess {
                    ReminderScheduler.scheduleWorkItemReminder(context, item.copy(completed = next))
                    _messages.tryEmit(if (next) "Marked complete" else "Reopened task")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to update task")
                }
        }
    }

    fun syncExistingTaskToCalendar(context: Context, item: WorkItem) {
        viewModelScope.launch {
            if (!CalendarHelper.hasCalendarPermissions(context)) {
                _messages.tryEmit("Grant Calendar permission first to sync directly")
                return@launch
            }
            val calId = _selectedCalendarId.value
                ?: CalendarHelper.queryWritableCalendars(context).firstOrNull()?.id
            if (calId == null) {
                _messages.tryEmit("No writable device calendar found")
                return@launch
            }
            _selectedCalendarId.value = calId
            val eventId = CalendarHelper.upsertWorkItemToCalendar(context, calId, item)
            if (eventId != null) {
                repository.updateWorkItemCalendarSync(currentUserId, item.id, eventId, true)
                    .onSuccess {
                        refreshCalendarState(context)
                        _messages.tryEmit("Synced \"${item.title}\" to Calendar")
                    }
                    .onFailure { err ->
                        _messages.tryEmit(err.localizedMessage ?: "Calendar synced, failed to update record")
                    }
            } else {
                _messages.tryEmit("Could not write event to Calendar")
            }
        }
    }

    fun importCalendarEventAsTask(context: Context, event: DeviceCalendarEvent, category: WorkCategory) {
        viewModelScope.launch {
            val isoDate = DateUtils.millisToIsoDate(event.startMillis)
            val startTime = if (event.allDay) "" else {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.startMillis }
                String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
            }
            val durationMin = ((event.endMillis - event.startMillis) / 60_000L).coerceIn(15L, 720L)
            val item = WorkItem(
                userId = currentUserId,
                title = event.title.take(120),
                notes = event.description.take(500),
                category = category.name,
                scheduledDate = isoDate,
                startTime = startTime,
                durationMinutes = durationMin,
                calendarEventId = event.id,
                calendarSynced = true
            )
            repository.saveWorkItem(currentUserId, item)
                .onSuccess { savedId ->
                    ReminderScheduler.scheduleWorkItemReminder(context, item.copy(id = savedId))
                    _messages.tryEmit("Imported \"${event.title}\" to DayLedger")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to import calendar event")
                }
        }
    }

    fun deleteWorkItem(context: Context, item: WorkItem) {
        viewModelScope.launch {
            repository.deleteWorkItem(currentUserId, item.id)
                .onSuccess {
                    ReminderScheduler.scheduleWorkItemReminder(context, item.copy(completed = true))
                    _messages.tryEmit("Task deleted")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to delete task")
                }
        }
    }

    fun saveClient(client: Client) {
        viewModelScope.launch {
            repository.saveClient(currentUserId, client)
                .onSuccess {
                    _messages.tryEmit(if (client.id.isBlank()) "Client added" else "Client updated")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to save client")
                }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(currentUserId, clientId)
                .onSuccess { _messages.tryEmit("Client removed") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Failed to delete client") }
        }
    }

    fun saveInvoice(context: Context, invoice: Invoice) {
        viewModelScope.launch {
            repository.saveInvoice(currentUserId, invoice)
                .onSuccess { savedId ->
                    ReminderScheduler.scheduleInvoiceReminder(context, invoice.copy(id = savedId))
                    _messages.tryEmit(if (invoice.id.isBlank()) "Invoice created" else "Invoice updated")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to save invoice")
                }
        }
    }

    fun recordInvoicePayment(context: Context, invoice: Invoice, newTotalPaid: Double) {
        viewModelScope.launch {
            repository.recordInvoicePayment(currentUserId, invoice, newTotalPaid)
                .onSuccess {
                    val isPaid = newTotalPaid >= invoice.amount
                    if (isPaid) {
                        ReminderScheduler.scheduleInvoiceReminder(
                            context,
                            invoice.copy(status = PaymentStatus.PAID.name, reminderEnabled = false)
                        )
                    }
                    _messages.tryEmit(
                        if (isPaid) "Invoice ${invoice.invoiceNumber} marked Paid"
                        else "Recorded payment on ${invoice.invoiceNumber}"
                    )
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to update payment")
                }
        }
    }

    fun toggleInvoiceReminder(context: Context, invoice: Invoice) {
        viewModelScope.launch {
            val next = !invoice.reminderEnabled
            repository.toggleInvoiceReminder(currentUserId, invoice, next)
                .onSuccess {
                    ReminderScheduler.scheduleInvoiceReminder(context, invoice.copy(reminderEnabled = next))
                    _messages.tryEmit(if (next) "Payment reminder enabled" else "Payment reminder paused")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to update reminder")
                }
        }
    }

    fun markInvoiceOverdue(invoice: Invoice) {
        viewModelScope.launch {
            repository.markInvoiceOverdue(currentUserId, invoice)
                .onSuccess { _messages.tryEmit("Marked ${invoice.invoiceNumber} as Overdue") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Failed to update status") }
        }
    }

    fun deleteInvoice(context: Context, invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(currentUserId, invoice.id)
                .onSuccess {
                    ReminderScheduler.scheduleInvoiceReminder(context, invoice.copy(reminderEnabled = false))
                    _messages.tryEmit("Invoice deleted")
                }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Failed to delete invoice")
                }
        }
    }

    private companion object {
        const val TAG = "DayLedgerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
