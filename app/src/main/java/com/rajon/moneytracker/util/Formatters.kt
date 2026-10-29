package com.rajon.moneytracker.util

import java.text.NumberFormat
import java.util.Locale

object Formatters {
    fun formatCurrency(amount: Double, symbol: String = "৳"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return "$symbol ${formatter.format(amount)}"
    }

    fun formatDate(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    fun formatMonthTitle(yearMonth: String): String {
        // yearMonth format: "YYYY-MM"
        try {
            val parts = yearMonth.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month - 1)
                }
                val sdf = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                return sdf.format(cal.time)
            }
        } catch (_: Exception) {}
        return yearMonth
    }
}
