package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class WorkCategory(val label: String, val shortLabel: String) {
    CLASS_BATCH("Class Batch", "Classes"),
    OFFICE_WORK("Office Work", "Office"),
    CLIENT_TASK("Client Task", "Clients"),
    PERSONAL("Daily Task", "Daily");

    companion object {
        fun fromRaw(raw: String?): WorkCategory =
            entries.firstOrNull { it.name == raw } ?: CLIENT_TASK
    }
}

enum class RecurrenceType(val label: String) {
    NONE("Once"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly");

    companion object {
        fun fromRaw(raw: String?): RecurrenceType =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class PaymentStatus(val label: String) {
    UNPAID("Unpaid"),
    PARTIAL("Partial"),
    PAID("Paid"),
    OVERDUE("Overdue");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID
    }
}

@Keep
@IgnoreExtraProperties
data class WorkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val category: String = WorkCategory.CLIENT_TASK.name,
    val scheduledDate: String = "",
    val startTime: String = "",
    val durationMinutes: Long = 60L,
    val recurrence: String = RecurrenceType.NONE.name,
    val completed: Boolean = false,
    val batchOrGroup: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val billable: Boolean = false,
    val billingAmount: Double = 0.0,
    val reminderMinutesBefore: Long = 15L,
    val calendarEventId: Long = -1L,
    val calendarSynced: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: WorkCategory get() = WorkCategory.fromRaw(category)
    val recurrenceEnum: RecurrenceType get() = RecurrenceType.fromRaw(recurrence)
}

@Keep
@IgnoreExtraProperties
data class Client(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val organization: String = "",
    val email: String = "",
    val phone: String = "",
    val defaultRate: Double = 0.0,
    val currencySymbol: String = "$",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val workTitle: String = "",
    val workItemId: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val currencySymbol: String = "$",
    val status: String = PaymentStatus.UNPAID.name,
    val dueDate: String = "",
    val paidDate: String = "",
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: PaymentStatus get() = PaymentStatus.fromRaw(status)
    val balanceRemaining: Double get() = (amount - paidAmount).coerceAtLeast(0.0)
}

data class DeviceCalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int
)

data class DeviceCalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
