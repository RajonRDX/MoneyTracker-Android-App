package com.rajon.moneytracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DonutChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier.size(200.dp)
) {
    val total = data.values.sum()
    if (total <= 0.0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "No expenses recorded", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val colors = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF8E24AA),
        Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF00ACC1), Color(0xFF7CB342)
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 40f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        data.entries.forEachIndexed { index, entry ->
            val sweep = ((entry.value / total) * 360f).toFloat()
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun DailyBarChart(
    dailyTotals: Map<Int, Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
) {
    val maxVal = dailyTotals.values.maxOrNull() ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.padding(8.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = (width / 31f).coerceAtLeast(4f)

        dailyTotals.forEach { (day, amount) ->
            val barHeight = if (maxVal > 0) (amount / maxVal * height).toFloat() else 0f
            val x = (day - 1) * (width / 31f)
            val y = height - barHeight

            drawRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth - 2f, barHeight)
            )
        }
    }
}

@Composable
fun SixMonthComparisonChart(
    months: List<String>,
    incomes: List<Double>,
    expenses: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(200.dp)
) {
    val maxVal = (incomes + expenses).maxOrNull() ?: 1.0
    val incomeColor = com.rajon.moneytracker.ui.theme.GreenSuccess
    val expenseColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val step = if (months.isNotEmpty()) width / months.size else width

        incomes.forEachIndexed { i, inc ->
            val x = i * step + step / 4f
            val barH = (inc / maxVal * height).toFloat()
            drawRect(
                color = incomeColor,
                topLeft = Offset(x, height - barH),
                size = Size(step / 3f, barH)
            )
        }

        expenses.forEachIndexed { i, exp ->
            val x = i * step + step / 2f
            val barH = (exp / maxVal * height).toFloat()
            drawRect(
                color = expenseColor,
                topLeft = Offset(x, height - barH),
                size = Size(step / 3f, barH)
            )
        }
    }
}

@Composable
fun CategoryTrendLineChart(
    trendValues: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp)
) {
    val maxVal = trendValues.maxOrNull() ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.padding(8.dp)) {
        val width = size.width
        val height = size.height
        val stepX = if (trendValues.size > 1) width / (trendValues.size - 1) else width

        for (i in 0 until trendValues.size - 1) {
            val x1 = i * stepX
            val y1 = height - (trendValues[i] / maxVal * height).toFloat()
            val x2 = (i + 1) * stepX
            val y2 = height - (trendValues[i + 1] / maxVal * height).toFloat()

            drawLine(
                color = lineColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4f
            )
        }
    }
}
