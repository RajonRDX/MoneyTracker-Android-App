package com.rajon.moneytracker.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.ExpenseCategory
import com.rajon.moneytracker.data.model.PaymentSource
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType
import com.rajon.moneytracker.ui.components.MonthSelectorBar
import com.rajon.moneytracker.ui.theme.BlueInfo
import com.rajon.moneytracker.ui.theme.GreenSuccess
import com.rajon.moneytracker.ui.theme.OrangeWarning
import com.rajon.moneytracker.ui.theme.RedError
import com.rajon.moneytracker.util.DateUtils
import com.rajon.moneytracker.util.Formatters
import java.util.UUID

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
    var deletingTx by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions
            .filter { tx ->
                val matchesType = selectedFilter == "ALL" || tx.type == selectedFilter
                val matchesQuery = searchQuery.isBlank() ||
                    tx.note.contains(searchQuery, ignoreCase = true) ||
                    tx.categoryName.contains(searchQuery, ignoreCase = true) ||
                    tx.paymentSource.contains(searchQuery, ignoreCase = true)
                matchesType && matchesQuery
            }
            .sortedByDescending { it.dateMillis }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add Expense / Tx") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MonthSelectorBar(
                currentYearMonth = currentYM,
                onPrevMonth = { viewModel.changeMonth(DateUtils.getPreviousYearMonth(currentYM)) },
                onNextMonth = { viewModel.changeMonth(DateUtils.getNextYearMonth(currentYM)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search food, bills, withdrawal, source...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income", "WITHDRAWAL" to "Withdrawals")
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions found.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap '+ Add Expense / Tx' to record food, bills, or ATM withdrawals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            tx = tx,
                            onEdit = { editingTx = tx },
                            onDelete = { deletingTx = tx }
                        )
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
            },
            onDelete = if (editingTx != null) {
                {
                    viewModel.deleteTransaction(editingTx!!.id)
                    editingTx = null
                }
            } else null
        )
    }

    if (deletingTx != null) {
        AlertDialog(
            onDismissRequest = { deletingTx = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text("Are you sure you want to delete '${deletingTx?.note?.ifBlank { deletingTx?.categoryName }}' (${Formatters.formatCurrency(deletingTx?.amount ?: 0.0)})? Your balances will update immediately.")
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
}

@Composable
fun TransactionItemCard(
    tx: Transaction,
    currencySymbol: String = "৳",
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = tx.type == TransactionType.INCOME.name
    val isWithdrawal = tx.type == TransactionType.WITHDRAWAL.name

    val badgeColor = when {
        isIncome -> GreenSuccess
        isWithdrawal -> BlueInfo
        tx.isExtra -> OrangeWarning
        else -> RedError
    }

    val icon = when {
        isIncome -> Icons.Default.TrendingUp
        isWithdrawal -> Icons.Default.Atm
        tx.categoryName.contains("Food", true) -> Icons.Default.Restaurant
        tx.categoryName.contains("Transport", true) -> Icons.Default.DirectionsCar
        tx.categoryName.contains("Shopping", true) -> Icons.Default.ShoppingBag
        else -> Icons.Default.Receipt
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.note.ifBlank {
                        if (isWithdrawal) "ATM / Cash Withdrawal" else tx.categoryName.ifBlank { tx.type }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tx.categoryName.ifBlank { tx.type }} • ${tx.paymentSource} • ${Formatters.formatDate(tx.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = when {
                    isIncome -> "+"
                    isWithdrawal -> "⇄ "
                    else -> "-"
                }
                Text(
                    text = "$prefix${Formatters.formatCurrency(tx.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    transaction: Transaction?,
    categories: List<ExpenseCategory>,
    currentYearMonth: String,
    initialType: String = TransactionType.EXPENSE.name,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: initialType) }
    var selectedSource by remember {
        mutableStateOf(
            transaction?.paymentSource ?: if (selectedType == TransactionType.WITHDRAWAL.name) PaymentSource.BANK.name else PaymentSource.CASH.name
        )
    }

    val defaultCatId = categories.firstOrNull { it.name.contains("Food", ignoreCase = true) }?.id
        ?: categories.firstOrNull()?.id
        ?: "food"

    var selectedCategoryId by remember {
        mutableStateOf(transaction?.categoryId?.ifBlank { defaultCatId } ?: defaultCatId)
    }
    var customCategoryName by remember { mutableStateOf("") }
    var isExtra by remember { mutableStateOf(transaction?.isExtra ?: false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (transaction == null) "Record Transaction" else "Modify Transaction",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Transaction Type Selector (High-contrast FilterChips)
            Text("Transaction Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf(
                    TransactionType.EXPENSE.name to "Expense",
                    TransactionType.INCOME.name to "Income / Salary",
                    TransactionType.WITHDRAWAL.name to "ATM Withdrawal"
                )
                types.forEach { (typeKey, label) ->
                    FilterChip(
                        selected = selectedType == typeKey,
                        onClick = {
                            selectedType = typeKey
                            if (typeKey == TransactionType.WITHDRAWAL.name) {
                                selectedSource = PaymentSource.BANK.name
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    errorText = null
                },
                label = { Text("Amount (৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        when (selectedType) {
                            TransactionType.EXPENSE.name -> "What did you buy? (e.g., Food, Groceries, Rickshaw)"
                            TransactionType.INCOME.name -> "Income Source (e.g., Monthly Salary, Bonus, Freelance)"
                            else -> "Withdrawal Note (e.g., ATM Cash Withdrawal)"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection for Expenses
            if (selectedType == TransactionType.EXPENSE.name) {
                Text("Select Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id && customCategoryName.isBlank(),
                            onClick = {
                                selectedCategoryId = cat.id
                                customCategoryName = ""
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customCategoryName,
                    onValueChange = { customCategoryName = it },
                    label = { Text("Or type custom category (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Method Selection
            if (selectedType != TransactionType.WITHDRAWAL.name) {
                Text(
                    text = if (selectedType == TransactionType.INCOME.name) "Deposit To" else "Paid Via",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaymentSource.values().toList()) { source ->
                        FilterChip(
                            selected = selectedSource == source.name,
                            onClick = { selectedSource = source.name },
                            label = { Text(source.displayName) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Transfers money directly from Bank Balance → Cash in Hand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueInfo
                )
            }

            if (selectedType == TransactionType.EXPENSE.name) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mark as Extra / Unplanned Expense", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = isExtra, onCheckedChange = { isExtra = it })
                }
            }

            if (errorText != null) {
                Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorText = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    val chosenCatObj = categories.find { it.id == selectedCategoryId }
                    val resolvedCatName = when {
                        selectedType == TransactionType.WITHDRAWAL.name -> "Cash Withdrawal"
                        selectedType == TransactionType.INCOME.name -> note.ifBlank { "Income" }
                        customCategoryName.isNotBlank() -> customCategoryName.trim()
                        else -> chosenCatObj?.name ?: "Food & Dining"
                    }

                    val resolvedNote = note.ifBlank { resolvedCatName }

                    onSave(
                        (transaction ?: Transaction(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis())).copy(
                            yearMonth = currentYearMonth,
                            amount = amt,
                            type = selectedType,
                            paymentSource = if (selectedType == TransactionType.WITHDRAWAL.name) PaymentSource.BANK.name else selectedSource,
                            categoryId = if (selectedType == TransactionType.EXPENSE.name) selectedCategoryId else selectedType.lowercase(),
                            categoryName = resolvedCatName,
                            note = resolvedNote,
                            isExtra = if (selectedType == TransactionType.EXPENSE.name) isExtra else false,
                            dateMillis = transaction?.dateMillis ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (transaction == null) "Save Transaction" else "Update Transaction", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}