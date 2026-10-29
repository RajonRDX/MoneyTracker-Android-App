package com.rajon.moneytracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rajon.moneytracker.MainActivity
import com.rajon.moneytracker.R

object NotificationHelper {
    private const val CHANNEL_REMINDERS_ID = "reminders_channel"
    private const val CHANNEL_BUDGET_ID = "budget_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameReminders = context.getString(R.string.notif_channel_reminders_name)
            val descReminders = context.getString(R.string.notif_channel_reminders_desc)
            val channelReminders = NotificationChannel(CHANNEL_REMINDERS_ID, nameReminders, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = descReminders
            }

            val nameBudget = context.getString(R.string.notif_channel_budget_name)
            val descBudget = context.getString(R.string.notif_channel_budget_desc)
            val channelBudget = NotificationChannel(CHANNEL_BUDGET_ID, nameBudget, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descBudget
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channelReminders)
            notificationManager.createNotificationChannel(channelBudget)
        }
    }

    fun showReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun showBudgetAlertNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
