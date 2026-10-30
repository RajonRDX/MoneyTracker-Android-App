package com.rajon.moneytracker.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ReceiptLong
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

    var showSetupDialog by remember { mutableStateOf(false) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(monthSetup) {
        if (monthSetup == null) {
            showSetupDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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
            }

            // Gradient Balance Card
            item {
                GradientCard {
                    Text(
                        text = "Bank Balance Left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                            Text(
                                text = "Cash in Hand",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Formatters.formatCurrency(breakdown.cashInHand, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Formatters.formatCurrency(breakdown.totalSpent, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
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

            // Quick Navigation Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transactions")
                    }
                    OutlinedButton(
                        onClick = onNavigateToBudget,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Budgets")
                    }
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
                                Text(
                                    text = "Due Day: ${rec.dueDay} • ${Formatters.formatCurrency(rec.amount, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
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

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating ATM Button
        FloatingActionButton(
            onClick = { showWithdrawalDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Atm, contentDescription = "Quick Withdrawal / ATM")
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