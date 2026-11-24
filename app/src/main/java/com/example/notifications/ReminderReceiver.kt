package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "DayLedger Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have a scheduled task or payment due."
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: CHANNEL_TASKS
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())

        ensureChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dayledger_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_TASKS = "dayledger_tasks_reminders"
        const val CHANNEL_PAYMENTS = "dayledger_payment_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val taskChannel = NotificationChannel(
                    CHANNEL_TASKS,
                    "Tasks & Class Batches",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for scheduled batches, office work, and client tasks."
                }
                val paymentChannel = NotificationChannel(
                    CHANNEL_PAYMENTS,
                    "Billing & Payment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for upcoming and overdue client invoice payments."
                }
                manager.createNotificationChannel(taskChannel)
                manager.createNotificationChannel(paymentChannel)
            }
        }
    }
}
