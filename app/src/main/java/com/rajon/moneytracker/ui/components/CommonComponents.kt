package com.rajon.moneytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rajon.moneytracker.ui.theme.TealDark
import com.rajon.moneytracker.ui.theme.TealPrimary

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(TealDark, TealPrimary)
                    )
                )
                .padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun MonthSelectorBar(
    currentYearMonth: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month"
            )
        }
        Text(
            text = com.rajon.moneytracker.util.Formatters.formatMonthTitle(currentYearMonth),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month"
            )
        }
    }
}

@Composable
fun BudgetProgressBar(
    title: String,
    spent: Double,
    limit: Double,
    currencySymbol: String = "৳"
) {
    val pct = if (limit > 0.0) ((spent / limit)).toFloat().coerceIn(0f, 1.5f) else 0f
    val color = when {
        pct >= 1f -> MaterialTheme.colorScheme.error
        pct >= 0.7f -> com.rajon.moneytracker.ui.theme.OrangeWarning
        else -> com.rajon.moneytracker.ui.theme.GreenSuccess
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${com.rajon.moneytracker.util.Formatters.formatCurrency(spent, currencySymbol)} / ${com.rajon.moneytracker.util.Formatters.formatCurrency(limit, currencySymbol)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
