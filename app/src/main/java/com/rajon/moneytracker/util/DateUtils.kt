package com.rajon.moneytracker.util

import java.util.Calendar
import java.util.Locale

object DateUtils {
    fun getCurrentYearMonth(): String {
        val cal = Calendar.getInstance()
        return formatYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun formatYearMonth(year: Int, month: Int): String {
        return String.format(Locale.US, "%d-%02d", year, month)
    }

    fun getPreviousYearMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size == 2) {
            var y = parts[0].toIntOrNull() ?: return yearMonth
            var m = (parts[1].toIntOrNull() ?: return yearMonth) - 1
            if (m < 1) {
                m = 12
                y -= 1
            }
            return formatYearMonth(y, m)
        }
        return yearMonth
    }

    fun getNextYearMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size == 2) {
            var y = parts[0].toIntOrNull() ?: return yearMonth
            var m = (parts[1].toIntOrNull() ?: return yearMonth) + 1
            if (m > 12) {
                m = 1
                y += 1
            }
            return formatYearMonth(y, m)
        }
        return yearMonth
    }

    fun getDaysLeftInMonth(): Int {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDay - today + 1).coerceAtLeast(1)
    }

    fun getDayOfMonth(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.DAY_OF_MONTH)
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