package com.rajon.moneytracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.MonthSetup
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType
import com.rajon.moneytracker.ui.components.*
import com.rajon.moneytracker.ui.screens.transactions.AddEditTransactionSheet
import com.rajon.moneytracker.ui.screens.transactions.TransactionItemCard
import com.rajon.moneytracker.ui.theme.BlueInfo
import com.rajon.moneytracker.ui.theme.GreenSuccess
import com.rajon.moneytracker.ui.theme.MintLight
import com.rajon.moneytracker.ui.theme.RedError
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
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val monthSetup by viewModel.monthSetup.collectAsStateWithLifecycle()
    val breakdown by viewModel.balanceBreakdown.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurringList.collectAsStateWithLifecycle()

    var showSetupDialog by remember { mutableStateOf(false) }
    var activeTxSheetType by remember { mutableStateOf<String?>(null) }
    var editingTx by remember { mutableStateOf<Transaction?>(null) }
    var deletingTx by remember { mutableStateOf<Transaction?>(null) }

    val categoryBreakdown = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryName.ifBlank { "General" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { it.dateMillis }.take(8)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { activeTxSheetType = TransactionType.EXPENSE.name },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Expense") },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            item {
                MonthSelectorBar(
                    currentYearMonth = currentYM,
                    onPrevMonth = { viewModel.changeMonth(DateUtils.getPreviousYearMonth(currentYM)) },
                    onNextMonth = { viewModel.changeMonth(DateUtils.getNextYearMonth(currentYM)) }
                )
            }

            // Dynamic Hero Balance Card
            item {
                GradientCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Available Balance (Bank + Cash)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatCurrency(
                                    breakdown.bankBalanceLeft + breakdown.cashInHand,
                                    settings.currencySymbol
                                ),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { showSetupDialog = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Monthly Salary & Balance")
                        }
                    }

                    if (breakdown.isCarriedOverFromPrevMonth) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "↻ Auto-carried over from previous month's closing balance/salary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroMetricItem("Bank Balance", breakdown.bankBalanceLeft, settings.currencySymbol)
                        HeroMetricItem("Cash in Hand", breakdown.cashInHand, settings.currencySymbol)
                        HeroMetricItem("Month Spent", breakdown.totalSpent, settings.currencySymbol)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val daysLeft = DateUtils.getDaysLeftInMonth()
                        val totalSpendable = (breakdown.bankBalanceLeft + breakdown.cashInHand).coerceAtLeast(0.0)
                        val safePerDay = if (daysLeft > 0) totalSpendable / daysLeft else 0.0
                        Text(
                            text = "Safe daily spend: ${Formatters.formatCurrency(safePerDay, settings.currencySymbol)}/day ($daysLeft days left)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintLight
                        )
                    }
                }
            }

            // Quick Action Buttons (Add Food/Expense, Add Income, ATM Withdraw, Setup)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Add Expense",
                        icon = Icons.Default.Restaurant,
                        color = RedError,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTxSheetType = TransactionType.EXPENSE.name }
                    )
                    QuickActionButton(
                        title = "Add Income",
                        icon = Icons.Default.AddCard,
                        color = GreenSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTxSheetType = TransactionType.INCOME.name }
                    )
                    QuickActionButton(
                        title = "ATM Withdraw",
                        icon = Icons.Default.Atm,
                        color = BlueInfo,
                        modifier = Modifier.weight(1f),
                        onClick = { activeTxSheetType = TransactionType.WITHDRAWAL.name }
                    )
                    QuickActionButton(
                        title = "Salary Setup",
                        icon = Icons.Default.AccountBalance,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { showSetupDialog = true }
                    )
                }
            }

            // This Month's Financial Summary Grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "This Month's Cashflow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onNavigateToBudget) {
                                Text("Manage Budgets →")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatBox("Opening Bank", breakdown.openingBankBalance, settings.currencySymbol, Modifier.weight(1f))
                            SummaryStatBox("Total Income", breakdown.totalIncome, settings.currencySymbol, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatBox("ATM Withdrawals", breakdown.totalWithdrawals, settings.currencySymbol, Modifier.weight(1f))
                            SummaryStatBox("Bank/Wallet Spent", breakdown.totalBankExpenses + breakdown.totalCardExpenses + breakdown.totalWalletExpenses, settings.currencySymbol, Modifier.weight(1f))
                        }

                        val overallLimit = monthSetup?.overallBudgetLimit ?: settings.overallMonthlyLimit
                        if (overallLimit > 0.0) {
                            BudgetProgressBar(
                                title = "Overall Monthly Budget",
                                spent = breakdown.totalSpent,
                                limit = overallLimit,
                                currencySymbol = settings.currencySymbol
                            )
                        }
                    }
                }
            }

            // Category Donut Chart (if expenses exist)
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Where Your Money Went",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DonutChart(data = categoryBreakdown, modifier = Modifier.size(160.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        Formatters.formatCurrency(amt, settings.currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Recurring Bills
            val unpaidRecurring = recurringList.filter { rec ->
                rec.active && !rec.paidMonths.contains(currentYM) && !rec.skippedMonths.contains(currentYM)
            }
            if (unpaidRecurring.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming Bills (${unpaidRecurring.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(unpaidRecurring) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Due Day ${rec.dueDay} • ${Formatters.formatCurrency(rec.amount, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                TextButton(onClick = { viewModel.markRecurringSkipped(rec) }) { Text("Skip") }
                                Button(onClick = { viewModel.markRecurringPaid(rec) }) { Text("Pay") }
                            }
                        }
                    }
                }
            }

            // Recent Activity (Expenses, Income, & Withdrawals with instant Edit/Delete)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Expenses & Withdrawals",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("See All")
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No expenses or withdrawals recorded this month yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { activeTxSheetType = TransactionType.EXPENSE.name }) {
                                Text("Record First Expense (e.g. Food)")
                            }
                        }
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { editingTx = tx },
                        onDelete = { deletingTx = tx }
                    )
                }
            }
        }
    }

    // Add or Edit Expense / Income / Withdrawal Bottom Sheet
    if (activeTxSheetType != null || editingTx != null) {
        AddEditTransactionSheet(
            transaction = editingTx,
            categories = categories,
            currentYearMonth = currentYM,
            initialType = activeTxSheetType ?: TransactionType.EXPENSE.name,
            onDismiss = {
                activeTxSheetType = null
                editingTx = null
            },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                activeTxSheetType = null
                editingTx = null
            },
            onDelete = if (editingTx != null) {
                {
                    viewModel.deleteTransaction(editingTx!!.id)
                    editingTx = null
                }
            } else null
        )
    }

    // Delete Confirmation Dialog
    if (deletingTx != null) {
        AlertDialog(
            onDismissRequest = { deletingTx = null },
            title = { Text("Delete Item?") },
            text = {
                Text("Remove '${deletingTx?.note?.ifBlank { deletingTx?.categoryName }}' (${Formatters.formatCurrency(deletingTx?.amount ?: 0.0, settings.currencySymbol)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(deletingTx!!.id)
                        deletingTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTx = null }) { Text("Cancel") }
            }
        )
    }

    if (showSetupDialog) {
        MonthSetupDialog(
            initial = monthSetup,
            fallbackBankBalance = breakdown.openingBankBalance,
            fallbackCashBalance = breakdown.openingCashBalance,
            fallbackSalary = breakdown.setupSalary,
            onDismiss = { showSetupDialog = false },
            onSave = { setup ->
                viewModel.saveMonthSetup(setup)
                showSetupDialog = false
            }
        )
    }
}

@Composable
private fun HeroMetricItem(label: String, amount: Double, currencySymbol: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = Formatters.formatCurrency(amount, currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryStatBox(label: String, amount: Double, symbol: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Formatters.formatCurrency(amount, symbol),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthSetupDialog(
    initial: MonthSetup?,
    fallbackBankBalance: Double = 0.0,
    fallbackCashBalance: Double = 0.0,
    fallbackSalary: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (MonthSetup) -> Unit
) {
    var salary by remember { mutableStateOf((initial?.salary ?: fallbackSalary).takeIf { it > 0 }?.toString() ?: "") }
    var extraInc by remember { mutableStateOf(initial?.extraIncome?.takeIf { it > 0 }?.toString() ?: "") }
    var openingBankBal by remember { mutableStateOf((initial?.openingBankBalance ?: fallbackBankBalance).takeIf { it > 0 }?.toString() ?: "") }
    var openingCashBal by remember { mutableStateOf((initial?.openingBalance ?: fallbackCashBalance).takeIf { it > 0 }?.toString() ?: "") }
    var overallLimit by remember { mutableStateOf(initial?.overallBudgetLimit?.takeIf { it > 0 }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Salary & Balance Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Set your base figures for this month. Leave any field blank to keep it at 0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary (Added to Bank)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = openingBankBal,
                    onValueChange = { openingBankBal = it },
                    label = { Text("Opening Bank Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = openingCashBal,
                    onValueChange = { openingCashBal = it },
                    label = { Text("Opening Cash in Hand") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = extraInc,
                    onValueChange = { extraInc = it },
                    label = { Text("Extra Income (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = overallLimit,
                    onValueChange = { overallLimit = it },
                    label = { Text("Overall Monthly Spending Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    MonthSetup(
                        salary = salary.toDoubleOrNull() ?: 0.0,
                        extraIncome = extraInc.toDoubleOrNull() ?: 0.0,
                        openingBankBalance = openingBankBal.toDoubleOrNull() ?: 0.0,
                        openingBalance = openingCashBal.toDoubleOrNull() ?: 0.0,
                        overallBudgetLimit = overallLimit.toDoubleOrNull() ?: 0.0
                    )
                )
            }) {
                Text("Save Setup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}