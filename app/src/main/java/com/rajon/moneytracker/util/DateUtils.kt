package com.rajon.moneytracker.util

import java.util.Calendar

object DateUtils {
    fun getCurrentYearMonth(): String {
        val cal = Calendar.getInstance()
        return formatYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun formatYearMonth(year: Int, month: Int): String {
        return String.format(java.util.Locale.US, "%d-%02d", year, month)
    }

    fun getDaysLeftInMonth(): Int {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDay - today).coerceAtLeast(1)
    }

    fun clampDueDay(year: Int, month: Int, dueDay: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return dueDay.coerceIn(1, maxDay)
    }
}
