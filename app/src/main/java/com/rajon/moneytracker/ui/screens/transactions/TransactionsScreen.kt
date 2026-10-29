package com.rajon.moneytracker.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.PaymentSource
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType
import com.rajon.moneytracker.ui.components.MonthSelectorBar
import com.rajon.moneytracker.util.DateUtils
import com.rajon.moneytracker.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val currentYM by viewModel.currentYearMonth.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No transactions found for this month.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteTransaction(tx.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { editingTx = tx }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.categoryName.ifBlank { tx.type },
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "${Formatters.formatDate(tx.dateMillis)} • ${tx.paymentSource}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (tx.note.isNotBlank()) {
                                            Text(text = tx.note, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatCurrency(tx.amount),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (tx.type == TransactionType.INCOME.name) com.rajon.moneytracker.ui.theme.GreenSuccess else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingTx != null) {
        AddEditTransactionSheet(
            transaction = editingTx,
            categories = categories,
            currentYearMonth = currentYM,
            onDismiss = {
                showAddDialog = false
                editingTx = null
            },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                showAddDialog = false
                editingTx = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    transaction: Transaction?,
    categories: List<com.rajon.moneytracker.data.model.ExpenseCategory>,
    currentYearMonth: String,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE.name) }
    var selectedSource by remember { mutableStateOf(transaction?.paymentSource ?: PaymentSource.CASH.name) }
    var selectedCategory by remember { mutableStateOf(transaction?.categoryId ?: (categories.firstOrNull()?.id ?: "")) }
    var isExtra by remember { mutableStateOf(transaction?.isExtra ?: false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (transaction == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedType = TransactionType.EXPENSE.name },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == TransactionType.EXPENSE.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Expense")
                }
                Button(
                    onClick = { selectedType = TransactionType.INCOME.name },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == TransactionType.INCOME.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Income")
                }
                Button(
                    onClick = { selectedType = TransactionType.WITHDRAWAL.name },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == TransactionType.WITHDRAWAL.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Withdrawal")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Extra (Unplanned Expense)")
                Switch(checked = isExtra, onCheckedChange = { isExtra = it })
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val catObj = categories.find { it.id == selectedCategory }
                    if (amt > 0.0) {
                        onSave(
                            (transaction ?: Transaction(id = java.util.UUID.randomUUID().toString())).copy(
                                yearMonth = currentYearMonth,
                                amount = amt,
                                type = selectedType,
                                paymentSource = selectedSource,
                                categoryId = selectedCategory,
                                categoryName = catObj?.name ?: "General",
                                note = note,
                                isExtra = isExtra,
                                dateMillis = transaction?.dateMillis ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Transaction")
            }
        }
    }
}
