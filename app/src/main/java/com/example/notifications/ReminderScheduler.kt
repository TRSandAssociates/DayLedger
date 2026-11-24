package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.Invoice
import com.example.model.PaymentStatus
import com.example.model.WorkItem
import com.example.util.DateUtils

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleWorkItemReminder(context: Context, item: WorkItem) {
        ReminderReceiver.ensureChannels(context)
        val requestCode = ("work_${item.id}").hashCode()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (item.completed || item.reminderMinutesBefore < 0L || item.scheduledDate.isBlank()) {
            cancelAlarm(context, alarmManager, requestCode)
            return
        }

        val eventStartMillis = DateUtils.parseDateAndTimeToMillis(
            isoDate = item.scheduledDate,
            timeHhMm = item.startTime,
            defaultHour = 9
        )
        val triggerAtMillis = eventStartMillis - item.reminderMinutesBefore * 60_000L
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val title = "${item.categoryEnum.label}: ${item.title}"
        val message = buildString {
            append(if (item.startTime.isNotBlank()) "Starts at ${DateUtils.formatTimeDisplay(item.startTime)}" else "Scheduled today")
            if (item.batchOrGroup.isNotBlank()) append(" · ${item.batchOrGroup}")
            if (item.clientName.isNotBlank()) append(" · ${item.clientName}")
        }

        val pendingIntent = buildPendingIntent(
            context = context,
            requestCode = requestCode,
            title = title,
            message = message,
            channelId = ReminderReceiver.CHANNEL_TASKS
        )
        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun scheduleInvoiceReminder(context: Context, invoice: Invoice) {
        ReminderReceiver.ensureChannels(context)
        val requestCode = ("inv_${invoice.id}").hashCode()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!invoice.reminderEnabled || invoice.statusEnum == PaymentStatus.PAID || invoice.dueDate.isBlank()) {
            cancelAlarm(context, alarmManager, requestCode)
            return
        }

        val triggerAtMillis = DateUtils.parseDateAndTimeToMillis(
            isoDate = invoice.dueDate,
            timeHhMm = "09:00"
        )
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val title = "Payment Due: ${invoice.clientName}"
        val message = "${invoice.invoiceNumber} (${DateUtils.formatCurrency(invoice.balanceRemaining, invoice.currencySymbol)}) due today for ${invoice.workTitle}."
        val pendingIntent = buildPendingIntent(
            context = context,
            requestCode = requestCode,
            title = title,
            message = message,
            channelId = ReminderReceiver.CHANNEL_PAYMENTS
        )
        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun showImmediateReminder(context: Context, title: String, message: String, isPayment: Boolean) {
        ReminderReceiver.ensureChannels(context)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(
                ReminderReceiver.EXTRA_CHANNEL_ID,
                if (isPayment) ReminderReceiver.CHANNEL_PAYMENTS else ReminderReceiver.CHANNEL_TASKS
            )
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        }
        context.sendBroadcast(intent)
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        title: String,
        message: String,
        channelId: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_CHANNEL_ID, channelId)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to schedule reminder alarm", e)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
