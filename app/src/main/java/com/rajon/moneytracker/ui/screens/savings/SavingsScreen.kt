package com.rajon.moneytracker.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.SavingsGoal
import com.rajon.moneytracker.util.Formatters

@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddGoalDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Savings Goal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = "Savings Goals", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (goals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(text = "No savings goals created yet. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedGoalForDeposit = goal }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = goal.name, style = MaterialTheme.typography.titleLarge)
                                    Text(text = "${(pct * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${Formatters.formatCurrency(goal.currentAmount)} / ${Formatters.formatCurrency(goal.targetAmount)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { name, target ->
                viewModel.saveGoal(SavingsGoal(name = name, targetAmount = target))
                showAddGoalDialog = false
            }
        )
    }

    if (selectedGoalForDeposit != null) {
        DepositDialog(
            goalName = selectedGoalForDeposit!!.name,
            onDismiss = { selectedGoalForDeposit = null },
            onConfirm = { amount, isWd, takeBank, note ->
                viewModel.addDeposit(selectedGoalForDeposit!!, amount, isWd, takeBank, note)
                selectedGoalForDeposit = null
            }
        )
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") }, singleLine = true)
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val t = target.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && t > 0) onSave(name, t)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DepositDialog(goalName: String, onDismiss: () -> Unit, onConfirm: (Double, Boolean, Boolean, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWithdrawal by remember { mutableStateOf(false) }
    var takeFromBank by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deposit / Withdraw for $goalName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isWithdrawal, onCheckedChange = { isWithdrawal = it })
                    Text("Is Withdrawal from Goal")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = takeFromBank, onCheckedChange = { takeFromBank = it })
                    Text("Deduct / Refund Bank Balance")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onConfirm(a, isWithdrawal, takeFromBank, note)
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
