package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Client
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.WorkItem
import com.example.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DayLedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user write." }
        return uid
    }

    private fun workItemsCollection(userId: String) =
        db.collection("users").document(userId).collection("workItems")

    private fun clientsCollection(userId: String) =
        db.collection("users").document(userId).collection("clients")

    private fun invoicesCollection(userId: String) =
        db.collection("users").document(userId).collection("invoices")

    fun observeWorkItems(userId: String): Flow<List<WorkItem>> = flow {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val path = "users/$userId/workItems"
        emitAll(
            workItemsCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeClients(userId: String): Flow<List<Client>> = flow {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val path = "users/$userId/clients"
        emitAll(
            clientsCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Client::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeInvoices(userId: String): Flow<List<Invoice>> = flow {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val path = "users/$userId/invoices"
        emitAll(
            invoicesCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Invoice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveWorkItem(userId: String, item: WorkItem): Result<String> {
        val isCreate = item.id.isBlank()
        var docPath = "users/$userId/workItems/${item.id.ifBlank { "new" }}"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = item.title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Title cannot be empty." }
            val cleanDate = item.scheduledDate.ifBlank { DateUtils.todayIso() }
            val duration = item.durationMinutes.coerceIn(5L, 720L)
            val amount = item.billingAmount.coerceIn(0.0, 10_000_000.0)
            val reminderMin = item.reminderMinutesBefore.coerceIn(-1L, 1440L)

            val docRef = if (isCreate) {
                workItemsCollection(uid).document()
            } else {
                workItemsCollection(uid).document(item.id)
            }
            docPath = docRef.path

            if (isCreate) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "notes" to item.notes.trim().take(500),
                    "category" to item.categoryEnum.name,
                    "scheduledDate" to cleanDate,
                    "startTime" to item.startTime.trim(),
                    "durationMinutes" to duration,
                    "recurrence" to item.recurrenceEnum.name,
                    "completed" to item.completed,
                    "batchOrGroup" to item.batchOrGroup.trim().take(80),
                    "clientId" to item.clientId.trim().take(128),
                    "clientName" to item.clientName.trim().take(80),
                    "billable" to item.billable,
                    "billingAmount" to amount,
                    "reminderMinutesBefore" to reminderMin,
                    "calendarEventId" to item.calendarEventId.coerceAtLeast(-1L),
                    "calendarSynced" to item.calendarSynced,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "notes" to item.notes.trim().take(500),
                    "category" to item.categoryEnum.name,
                    "scheduledDate" to cleanDate,
                    "startTime" to item.startTime.trim(),
                    "durationMinutes" to duration,
                    "recurrence" to item.recurrenceEnum.name,
                    "completed" to item.completed,
                    "batchOrGroup" to item.batchOrGroup.trim().take(80),
                    "clientId" to item.clientId.trim().take(128),
                    "clientName" to item.clientName.trim().take(80),
                    "billable" to item.billable,
                    "billingAmount" to amount,
                    "reminderMinutesBefore" to reminderMin,
                    "calendarEventId" to item.calendarEventId.coerceAtLeast(-1L),
                    "calendarSynced" to item.calendarSynced,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleWorkItemCompleted(userId: String, itemId: String, completed: Boolean): Result<Unit> {
        val docPath = "users/$userId/workItems/$itemId"
        return try {
            val uid = requireUserId(userId)
            require(itemId.isNotBlank()) { "Task ID cannot be blank" }
            workItemsCollection(uid).document(itemId).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateWorkItemCalendarSync(
        userId: String,
        itemId: String,
        calendarEventId: Long,
        synced: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/workItems/$itemId"
        return try {
            val uid = requireUserId(userId)
            require(itemId.isNotBlank()) { "Task ID cannot be blank" }
            workItemsCollection(uid).document(itemId).update(
                mapOf(
                    "calendarEventId" to calendarEventId.coerceAtLeast(-1L),
                    "calendarSynced" to synced,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkItem(userId: String, itemId: String): Result<Unit> {
        val docPath = "users/$userId/workItems/$itemId"
        return try {
            val uid = requireUserId(userId)
            require(itemId.isNotBlank()) { "Task ID cannot be blank" }
            workItemsCollection(uid).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveClient(userId: String, client: Client): Result<String> {
        val isCreate = client.id.isBlank()
        var docPath = "users/$userId/clients/${client.id.ifBlank { "new" }}"
        return try {
            val uid = requireUserId(userId)
            val cleanName = client.name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Client name cannot be empty." }
            val rate = client.defaultRate.coerceIn(0.0, 10_000_000.0)
            val currency = client.currencySymbol.trim().ifBlank { "$" }.take(5)

            val docRef = if (isCreate) {
                clientsCollection(uid).document()
            } else {
                clientsCollection(uid).document(client.id)
            }
            docPath = docRef.path

            if (isCreate) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "organization" to client.organization.trim().take(80),
                        "email" to client.email.trim().take(120),
                        "phone" to client.phone.trim().take(30),
                        "defaultRate" to rate,
                        "currencySymbol" to currency,
                        "notes" to client.notes.trim().take(400),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "organization" to client.organization.trim().take(80),
                        "email" to client.email.trim().take(120),
                        "phone" to client.phone.trim().take(30),
                        "defaultRate" to rate,
                        "currencySymbol" to currency,
                        "notes" to client.notes.trim().take(400),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(userId: String, clientId: String): Result<Unit> {
        val docPath = "users/$userId/clients/$clientId"
        return try {
            val uid = requireUserId(userId)
            require(clientId.isNotBlank()) { "Client ID cannot be blank" }
            clientsCollection(uid).document(clientId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveInvoice(userId: String, invoice: Invoice): Result<String> {
        val isCreate = invoice.id.isBlank()
        var docPath = "users/$userId/invoices/${invoice.id.ifBlank { "new" }}"
        return try {
            val uid = requireUserId(userId)
            val cleanClient = invoice.clientName.trim().take(80)
            val cleanTitle = invoice.workTitle.trim().take(120)
            require(cleanClient.isNotEmpty()) { "Client name is required." }
            require(cleanTitle.isNotEmpty()) { "Work title is required." }
            require(invoice.amount > 0.0 && invoice.amount <= 10_000_000.0) { "Amount must be greater than 0." }

            val clampedPaid = invoice.paidAmount.coerceIn(0.0, invoice.amount)
            val computedStatus = when {
                clampedPaid >= invoice.amount -> PaymentStatus.PAID.name
                clampedPaid > 0.0 -> PaymentStatus.PARTIAL.name
                invoice.statusEnum == PaymentStatus.OVERDUE -> PaymentStatus.OVERDUE.name
                else -> PaymentStatus.UNPAID.name
            }
            val paidDate = if (computedStatus == PaymentStatus.PAID.name) {
                invoice.paidDate.ifBlank { DateUtils.todayIso() }
            } else {
                ""
            }
            val dueDate = invoice.dueDate.ifBlank { DateUtils.offsetDaysIso(DateUtils.todayIso(), 7) }
            val number = invoice.invoiceNumber.trim().ifBlank {
                "INV-${(1000..9999).random()}"
            }.take(32)
            val currency = invoice.currencySymbol.trim().ifBlank { "$" }.take(5)

            val docRef = if (isCreate) {
                invoicesCollection(uid).document()
            } else {
                invoicesCollection(uid).document(invoice.id)
            }
            docPath = docRef.path

            if (isCreate) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "invoiceNumber" to number,
                        "clientId" to invoice.clientId.trim().take(128),
                        "clientName" to cleanClient,
                        "workTitle" to cleanTitle,
                        "workItemId" to invoice.workItemId.trim().take(128),
                        "amount" to invoice.amount,
                        "paidAmount" to clampedPaid,
                        "currencySymbol" to currency,
                        "status" to computedStatus,
                        "dueDate" to dueDate,
                        "paidDate" to paidDate,
                        "notes" to invoice.notes.trim().take(400),
                        "reminderEnabled" to (computedStatus != PaymentStatus.PAID.name && invoice.reminderEnabled),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "invoiceNumber" to number,
                        "clientId" to invoice.clientId.trim().take(128),
                        "clientName" to cleanClient,
                        "workTitle" to cleanTitle,
                        "amount" to invoice.amount,
                        "paidAmount" to clampedPaid,
                        "currencySymbol" to currency,
                        "status" to computedStatus,
                        "dueDate" to dueDate,
                        "paidDate" to paidDate,
                        "notes" to invoice.notes.trim().take(400),
                        "reminderEnabled" to (computedStatus != PaymentStatus.PAID.name && invoice.reminderEnabled),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordInvoicePayment(
        userId: String,
        invoice: Invoice,
        totalPaidAmount: Double
    ): Result<Unit> {
        val docPath = "users/$userId/invoices/${invoice.id}"
        return try {
            val uid = requireUserId(userId)
            require(invoice.id.isNotBlank()) { "Invoice ID cannot be blank" }
            val clamped = totalPaidAmount.coerceIn(0.0, invoice.amount)
            val newStatus = when {
                clamped >= invoice.amount -> PaymentStatus.PAID.name
                clamped > 0.0 -> PaymentStatus.PARTIAL.name
                DateUtils.isDateBeforeToday(invoice.dueDate) -> PaymentStatus.OVERDUE.name
                else -> PaymentStatus.UNPAID.name
            }
            val newPaidDate = if (newStatus == PaymentStatus.PAID.name) DateUtils.todayIso() else ""
            val reminderOn = newStatus != PaymentStatus.PAID.name && invoice.reminderEnabled

            invoicesCollection(uid).document(invoice.id).update(
                mapOf(
                    "paidAmount" to clamped,
                    "status" to newStatus,
                    "paidDate" to newPaidDate,
                    "reminderEnabled" to reminderOn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleInvoiceReminder(
        userId: String,
        invoice: Invoice,
        enabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/invoices/${invoice.id}"
        return try {
            val uid = requireUserId(userId)
            require(invoice.id.isNotBlank()) { "Invoice ID cannot be blank" }
            invoicesCollection(uid).document(invoice.id).update(
                mapOf(
                    "paidAmount" to invoice.paidAmount,
                    "status" to invoice.statusEnum.name,
                    "paidDate" to invoice.paidDate,
                    "reminderEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markInvoiceOverdue(userId: String, invoice: Invoice): Result<Unit> {
        val docPath = "users/$userId/invoices/${invoice.id}"
        return try {
            val uid = requireUserId(userId)
            require(invoice.id.isNotBlank()) { "Invoice ID cannot be blank" }
            invoicesCollection(uid).document(invoice.id).update(
                mapOf(
                    "paidAmount" to invoice.paidAmount,
                    "status" to PaymentStatus.OVERDUE.name,
                    "paidDate" to "",
                    "reminderEnabled" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePaidInvoiceNotes(userId: String, invoiceId: String, notes: String): Result<Unit> {
        val docPath = "users/$userId/invoices/$invoiceId"
        return try {
            val uid = requireUserId(userId)
            require(invoiceId.isNotBlank()) { "Invoice ID cannot be blank" }
            invoicesCollection(uid).document(invoiceId).update(
                mapOf(
                    "notes" to notes.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteInvoice(userId: String, invoiceId: String): Result<Unit> {
        val docPath = "users/$userId/invoices/$invoiceId"
        return try {
            val uid = requireUserId(userId)
            require(invoiceId.isNotBlank()) { "Invoice ID cannot be blank" }
            invoicesCollection(uid).document(invoiceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
