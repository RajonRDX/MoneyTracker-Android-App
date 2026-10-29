package com.rajon.moneytracker.ui.screens.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.ExpenseCategory
import com.rajon.moneytracker.ui.components.BudgetProgressBar
import com.rajon.moneytracker.ui.components.MonthSelectorBar
import com.rajon.moneytracker.util.DateUtils

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val currentYM by viewModel.currentYearMonth.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val budgetStatuses by viewModel.budgetStatuses.collectAsStateWithLifecycle()

    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MonthSelectorBar(
            currentYearMonth = currentYM,
            onPrevMonth = {
                val parts = currentYM.split("-")
                if (parts.size == 2) {
                    var y = parts[0].toInt()
                    var m = parts[1].toInt() - 1
                    if (m < 1) { m = 12; y -= 1 }
                    viewModel.changeMonth(DateUtils.formatYearMonth(y, m))
                }
            },
            onNextMonth = {
                val parts = currentYM.split("-")
                if (parts.size == 2) {
                    var y = parts[0].toInt()
                    var m = parts[1].toInt() + 1
                    if (m > 12) { m = 1; y += 1 }
                    viewModel.changeMonth(DateUtils.formatYearMonth(y, m))
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Category Budgets", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { cat ->
                val status = budgetStatuses.find { it.categoryId == cat.id }
                val spent = status?.spentAmount ?: 0.0
                val limit = status?.limitAmount ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { editingCategory = cat }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = cat.name, style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { editingCategory = cat }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Limit")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        BudgetProgressBar(title = "", spent = spent, limit = limit)
                    }
                }
            }
        }
    }

    if (editingCategory != null) {
        val cat = editingCategory!!
        val currentStatus = budgetStatuses.find { it.categoryId == cat.id }
        var limitInput by remember { mutableStateOf(currentStatus?.limitAmount?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Set Budget for ${cat.name}") },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Monthly Limit Amount") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val lim = limitInput.toDoubleOrNull() ?: 0.0
                    viewModel.saveBudget(cat.id, cat.name, lim)
                    editingCategory = null
                }) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("Cancel") }
            }
        )
    }
}
