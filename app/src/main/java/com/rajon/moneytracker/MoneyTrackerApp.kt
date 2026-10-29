package com.rajon.moneytracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.rajon.moneytracker.data.di.AppContainer
import com.rajon.moneytracker.worker.NotificationHelper
import com.rajon.moneytracker.worker.DailyReminderWorker

class MoneyTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Enable Firestore offline persistence
        val firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            )
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings

        container = AppContainer(this)

        // Create notification channels and schedule periodic WorkManager reminder
        NotificationHelper.createNotificationChannels(this)
        DailyReminderWorker.scheduleWork(this)
    }
}
