package com.rajon.moneytracker.data.local

import android.content.Context
import android.content.SharedPreferences

class LocalPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("money_tracker_local_prefs", Context.MODE_PRIVATE)

    var isLocked: Boolean
        get() = prefs.getBoolean("is_locked", false)
        set(value) = prefs.edit().putBoolean("is_locked", value).apply()

    var lastNotifiedDate: String
        get() = prefs.getString("last_notified_date", "") ?: ""
        set(value) = prefs.edit().putString("last_notified_date", value).apply()

    var lastClosedMonth: String
        get() = prefs.getString("last_closed_month", "") ?: ""
        set(value) = prefs.edit().putString("last_closed_month", value).apply()
}
