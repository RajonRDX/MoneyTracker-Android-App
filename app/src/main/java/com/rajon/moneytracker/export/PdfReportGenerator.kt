package com.rajon.moneytracker.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.rajon.moneytracker.data.model.MonthlySummary
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {
    fun generateMonthlyPdf(summary: MonthlySummary, file: File): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint().apply {
                textSize = 14f
                color = android.graphics.Color.BLACK
            }

            var y = 50f
            canvas.drawText("MoneyTracker - Monthly Closing Report (${summary.yearMonth})", 50f, y, paint.apply { textSize = 18f; isFakeBoldText = true })
            y += 40f
            canvas.drawText("Total Income: ${summary.totalIncome}", 50f, y, paint.apply { textSize = 14f; isFakeBoldText = false })
            y += 25f
            canvas.drawText("Total Spent: ${summary.totalSpent}", 50f, y, paint.parseColorIfNeeded())
            y += 25f
            canvas.drawText("Saved Amount: ${summary.savedAmount}", 50f, y, paint)
            y += 25f
            canvas.drawText("Closing Bank Balance: ${summary.closingBankBalance}", 50f, y, paint)
            y += 25f
            canvas.drawText("Closing Cash in Hand: ${summary.closingCashInHand}", 50f, y, paint)

            pdfDocument.finishPage(page)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

private fun Paint.parseColorIfNeeded(): Paint {
    this.isFakeBoldText = false
    return this
}
