package com.rajon.moneytracker.ui.screens.debts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.DebtItem
import com.rajon.moneytracker.data.model.DebtType
import com.rajon.moneytracker.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onBack: () -> Unit,
    viewModel: DebtsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debts & Loans Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (debts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(text = "No active loans or debts recorded.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(debts, key = { it.id }) { debt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedDebtForPayment = debt }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = debt.personName, style = MaterialTheme.typography.titleLarge)
                                    Text(text = debt.type, style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Paid: ${Formatters.formatCurrency(debt.paidAmount)} / Total: ${Formatters.formatCurrency(debt.totalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Status: ${debt.status}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, total, type ->
                viewModel.saveDebt(DebtItem(personName = name, totalAmount = total, type = type))
                showAddDialog = false
            }
        )
    }

    if (selectedDebtForPayment != null) {
        DebtPaymentDialog(
            debtName = selectedDebtForPayment!!.personName,
            onDismiss = { selectedDebtForPayment = null },
            onConfirm = { amount, source, note, createExp ->
                viewModel.addPayment(selectedDebtForPayment!!, amount, source, note, createExp)
                selectedDebtForPayment = null
            }
        )
    }
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var isIowe by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loan / Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Person / Lender Name") }, singleLine = true)
                OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total Amount") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isIowe, onCheckedChange = { isIowe = it })
                    Text("I Owe (Borrowing)")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val t = total.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && t > 0) {
                    onSave(name, t, if (isIowe) DebtType.I_OWE.name else DebtType.OWED_TO_ME.name)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DebtPaymentDialog(debtName: String, onDismiss: () -> Unit, onConfirm: (Double, String, String, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var createExp by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for $debtName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Payment Amount") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = createExp, onCheckedChange = { createExp = it })
                    Text("Create Expense in Debt Payment")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onConfirm(a, "BANK", note, createExp)
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
