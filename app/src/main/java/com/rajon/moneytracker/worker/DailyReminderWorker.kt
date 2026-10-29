package com.rajon.moneytracker.worker

import android.content.Context
import androidx.work.*
import com.rajon.moneytracker.data.repository.FirestoreFinanceRepository
import com.rajon.moneytracker.util.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repo = FirestoreFinanceRepository()
            val snapshot = repo.getSnapshotForWorker()
            val currentYM = DateUtils.getCurrentYearMonth()
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            // Check recurring payments due soon or overdue
            for (rec in snapshot.recurringList) {
                if (!rec.active) continue
                if (rec.paidMonths.contains(currentYM) || rec.skippedMonths.contains(currentYM)) continue

                val dueDay = rec.dueDay
                val diff = dueDay - currentDay
                if (diff in 0..rec.remindDaysBefore || diff < 0) {
                    val statusText = if (diff < 0) "Overdue by ${-diff} days!" else "Due in $diff days"
                    NotificationHelper.showReminderNotification(
                        applicationContext,
                        "Recurring Bill Reminder",
                        "${rec.name} (${rec.amount}) is $statusText",
                        rec.id.hashCode()
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyMoneyTrackerReminderWork"

        fun scheduleWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyRequest
            )
        }
    }
}
