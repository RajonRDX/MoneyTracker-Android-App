package com.rajon.moneytracker.export

import com.rajon.moneytracker.data.model.Transaction
import java.io.File
import java.io.FileWriter

object CsvExporter {
    fun exportTransactionsToCsv(transactions: List<Transaction>, file: File): Boolean {
        return try {
            FileWriter(file).use { writer ->
                writer.append("ID,YearMonth,Type,Amount,DateMillis,CategoryName,PaymentSource,Note,IsExtra\n")
                for (tx in transactions) {
                    writer.append("${tx.id},${tx.yearMonth},${tx.type},${tx.amount},${tx.dateMillis},\"${tx.categoryName}\",${tx.paymentSource},\"${tx.note}\",${tx.isExtra}\n")
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
