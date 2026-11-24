package com.example.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.DeviceCalendarEvent
import com.example.model.DeviceCalendarInfo
import com.example.model.Invoice
import com.example.model.RecurrenceType
import com.example.model.WorkItem
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

object CalendarHelper {
    private const val TAG = "CalendarHelper"

    fun hasCalendarPermissions(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    suspend fun queryWritableCalendars(context: Context): List<DeviceCalendarInfo> =
        withContext(Dispatchers.IO) {
            if (!hasCalendarPermissions(context)) return@withContext emptyList()
            val calendars = mutableListOf<DeviceCalendarInfo>()
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            )
            val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Calendars.IS_PRIMARY} DESC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                    val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                    while (cursor.moveToNext()) {
                        calendars.add(
                            DeviceCalendarInfo(
                                id = cursor.getLong(idIdx),
                                displayName = cursor.getString(nameIdx) ?: "Device Calendar",
                                accountName = cursor.getString(accountIdx) ?: "",
                                color = cursor.getInt(colorIdx)
                            )
                        )
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Missing calendar permission while querying calendars", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query device calendars", e)
            }
            calendars
        }

    suspend fun queryEventsForRange(
        context: Context,
        startMillis: Long,
        endMillis: Long
    ): List<DeviceCalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext emptyList()
        val events = mutableListOf<DeviceCalendarEvent>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleIdx).orEmpty()
                    if (title.isNotBlank()) {
                        events.add(
                            DeviceCalendarEvent(
                                id = cursor.getLong(idIdx),
                                calendarId = cursor.getLong(calIdIdx),
                                title = title,
                                description = cursor.getString(descIdx).orEmpty(),
                                startMillis = cursor.getLong(beginIdx),
                                endMillis = cursor.getLong(endIdx),
                                allDay = cursor.getInt(allDayIdx) == 1
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing calendar permission while querying events", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query device calendar events", e)
        }
        events
    }

    suspend fun upsertWorkItemToCalendar(
        context: Context,
        calendarId: Long,
        workItem: WorkItem
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context) || calendarId <= 0L) return@withContext null

        val startMillis = DateUtils.parseDateAndTimeToMillis(
            isoDate = workItem.scheduledDate,
            timeHhMm = workItem.startTime,
            defaultHour = 9
        )
        val durationMin = workItem.durationMinutes.coerceIn(15L, 720L)
        val endMillis = startMillis + durationMin * 60_000L

        val descParts = buildList {
            add("Category: ${workItem.categoryEnum.label}")
            if (workItem.batchOrGroup.isNotBlank()) add("Batch / Group: ${workItem.batchOrGroup}")
            if (workItem.clientName.isNotBlank()) add("Client: ${workItem.clientName}")
            if (workItem.billable && workItem.billingAmount > 0.0) {
                add("Billable Fee: ${DateUtils.formatCurrency(workItem.billingAmount)}")
            }
            if (workItem.notes.isNotBlank()) add(workItem.notes)
            add("Synced by DayLedger")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, workItem.title)
            put(CalendarContract.Events.DESCRIPTION, descParts.joinToString("\n"))
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.ALL_DAY, if (workItem.startTime.isBlank()) 1 else 0)

            val rrule = when (workItem.recurrenceEnum) {
                RecurrenceType.DAILY -> "FREQ=DAILY"
                RecurrenceType.WEEKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
                RecurrenceType.WEEKLY -> "FREQ=WEEKLY"
                RecurrenceType.NONE -> null
            }
            if (rrule != null) {
                put(CalendarContract.Events.RRULE, rrule)
                put(CalendarContract.Events.DURATION, "PT${durationMin}M")
                putNull(CalendarContract.Events.DTEND)
            } else {
                putNull(CalendarContract.Events.RRULE)
                putNull(CalendarContract.Events.DURATION)
                put(CalendarContract.Events.DTEND, endMillis)
            }
        }

        try {
            if (workItem.calendarEventId > 0L) {
                val updateUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    workItem.calendarEventId
                )
                val updated = context.contentResolver.update(updateUri, values, null, null)
                if (updated > 0) {
                    return@withContext workItem.calendarEventId
                }
            }

            val insertedUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val newId = insertedUri?.lastPathSegment?.toLongOrNull()
            if (newId != null && workItem.reminderMinutesBefore >= 0L) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, newId)
                    put(CalendarContract.Reminders.MINUTES, workItem.reminderMinutesBefore.toInt())
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            newId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert event to Android Calendar", e)
            null
        }
    }

    fun buildInsertCalendarIntent(workItem: WorkItem): Intent {
        val startMillis = DateUtils.parseDateAndTimeToMillis(
            isoDate = workItem.scheduledDate,
            timeHhMm = workItem.startTime,
            defaultHour = 9
        )
        val endMillis = startMillis + workItem.durationMinutes.coerceIn(15L, 720L) * 60_000L
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, workItem.title)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                buildString {
                    append(workItem.categoryEnum.label)
                    if (workItem.clientName.isNotBlank()) append(" · ${workItem.clientName}")
                    if (workItem.notes.isNotBlank()) append("\n${workItem.notes}")
                }
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, workItem.startTime.isBlank())
        }
    }

    fun buildInvoiceDueCalendarIntent(invoice: Invoice): Intent {
        val startMillis = DateUtils.parseDateAndTimeToMillis(
            isoDate = invoice.dueDate,
            timeHhMm = "10:00"
        )
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(
                CalendarContract.Events.TITLE,
                "Payment Due: ${invoice.clientName} (${DateUtils.formatCurrency(invoice.balanceRemaining, invoice.currencySymbol)})"
            )
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Invoice ${invoice.invoiceNumber} · ${invoice.workTitle}"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 30 * 60_000L)
        }
    }
}
