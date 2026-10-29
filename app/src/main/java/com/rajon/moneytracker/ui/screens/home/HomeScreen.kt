package com.rajon.moneytracker.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.MonthSetup
import com.rajon.moneytracker.ui.components.*
import com.rajon.moneytracker.util.DateUtils
import com.rajon.moneytracker.util.Formatters

@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val currentYM by viewModel.currentYearMonth.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val monthSetup by viewModel.monthSetup.collectAsStateWithLifecycle()
    val breakdown by viewModel.balanceBreakdown.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurringList.collectAsStateWithLifecycle()
    val debtsList by viewModel.debtsList.collectAsStateWithLifecycle()

    var showSetupDialog by remember { mutableStateOf(false) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    // If month setup is null, trigger dialog to set opening balance & salary
    LaunchedEffect(monthSetup) {
        if (monthSetup == null) {
            showSetupDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showWithdrawalDialog = true }) {
                Icon(Icons.Default.Atm, contentDescription = "Quick Withdrawal / ATM")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelectorBar(
                    currentYearMonth = currentYM,
                    onPrevMonth = {
                        // Simple previous month shift
                        val parts = currentYM.split("-")
                        if (parts.size == 2) {
                            var y = parts[0].toInt()
                            var m = parts[1].toInt() - 1
                            if (m < 1) {
                                m = 12
                                y -= 1
                            }
                            viewModel.changeMonth(DateUtils.formatYearMonth(y, m))
                        }
                    },
                    onNextMonth = {
                        val parts = currentYM.split("-")
                        if (parts.size == 2) {
                            var y = parts[0].toInt()
                            var m = parts[1].toInt() + 1
                            if (m > 12) {
                                m = 1
                                y += 1
                            }
                            viewModel.changeMonth(DateUtils.formatYearMonth(y, m))
                        }
                    }
                )
            }

            // Gradient Balance Card
            item {
                GradientCard {
                    Text(text = "Bank Balance Left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatCurrency(breakdown.bankBalanceLeft, settings.currencySymbol),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Cash in Hand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = Formatters.formatCurrency(breakdown.cashInHand, settings.currencySymbol), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text(text = "Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = Formatters.formatCurrency(breakdown.totalSpent, settings.currencySymbol), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val daysLeft = DateUtils.getDaysLeftInMonth()
                    val safePerDay = if (daysLeft > 0) (breakdown.bankBalanceLeft / daysLeft).coerceAtLeast(0.0) else 0.0
                    Text(
                        text = "Safe to spend: ${Formatters.formatCurrency(safePerDay, settings.currencySymbol)} / day ($daysLeft days left)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Month Setup Trigger Button if missing
            if (monthSetup == null) {
                item {
                    Button(
                        onClick = { showSetupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Salary & Opening Balance for This Month")
                    }
                }
            }

            // Upcoming Recurring Payments Section
            val unpaidRecurring = recurringList.filter { rec ->
                rec.active && !rec.paidMonths.contains(currentYM) && !rec.skippedMonths.contains(currentYM)
            }
            if (unpaidRecurring.isNotEmpty()) {
                item {
                    Text(text = "Upcoming & Overdue Payments", style = MaterialTheme.typography.titleLarge)
                }
                items(unpaidRecurring) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = rec.name, style = MaterialTheme.typography.titleLarge)
                                Text(text = "Due Day: ${rec.dueDay} • ${Formatters.formatCurrency(rec.amount, settings.currencySymbol)}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row {
                                TextButton(onClick = { viewModel.markRecurringSkipped(rec) }) {
                                    Text("Skip")
                                }
                                Button(onClick = { viewModel.markRecurringPaid(rec) }) {
                                    Text("Pay")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        MonthSetupDialog(
            initial = monthSetup,
            onDismiss = { showSetupDialog = false },
            onSave = { setup ->
                viewModel.saveMonthSetup(setup)
                showSetupDialog = false
            }
        )
    }

    if (showWithdrawalDialog) {
        QuickWithdrawalDialog(
            onDismiss = { showWithdrawalDialog = false },
            onConfirm = { amt ->
                viewModel.recordQuickWithdrawal(amt)
                showWithdrawalDialog = false
            }
        )
    }
}

@Composable
fun MonthSetupDialog(
    initial: MonthSetup?,
    onDismiss: () -> Unit,
    onSave: (MonthSetup) -> Unit
) {
    var salary by remember { mutableStateOf(initial?.salary?.toString() ?: "") }
    var extraInc by remember { mutableStateOf(initial?.extraIncome?.toString() ?: "") }
    var openingBal by remember { mutableStateOf(initial?.openingBankBalance?.toString() ?: "") }
    var overallLimit by remember { mutableStateOf(initial?.overallBudgetLimit?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Budget & Balance Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = extraInc,
                    onValueChange = { extraInc = it },
                    label = { Text("Extra Income (Optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = openingBal,
                    onValueChange = { openingBal = it },
                    label = { Text("Opening Bank Balance") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = overallLimit,
                    onValueChange = { overallLimit = it },
                    label = { Text("Overall Spending Limit (Optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = salary.toDoubleOrNull() ?: 0.0
                val e = extraInc.toDoubleOrNull() ?: 0.0
                val o = openingBal.toDoubleOrNull() ?: 0.0
                val l = overallLimit.toDoubleOrNull() ?: 0.0
                onSave(
                    MonthSetup(
                        salary = s,
                        extraIncome = e,
                        openingBankBalance = o,
                        overallBudgetLimit = l
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickWithdrawalDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record ATM / Bank Withdrawal") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Withdrawal Amount") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0.0) onConfirm(amt)
            }) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
