package com.rajon.moneytracker.ui.screens.cards

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.data.model.*
import com.rajon.moneytracker.ui.components.GradientCard
import com.rajon.moneytracker.ui.theme.*
import com.rajon.moneytracker.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    viewModel: CreditCardsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summaries by viewModel.cardSummaries.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddEditCardDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var deletingCard by remember { mutableStateOf<CreditCard?>(null) }

    var purchaseCard by remember { mutableStateOf<CreditCard?>(null) }
    var loanCard by remember { mutableStateOf<CreditCard?>(null) }
    var paymentCardSummary by remember { mutableStateOf<CardFinancialSummary?>(null) }

    val totalDebtAllCards = summaries.sumOf { it.totalOutstandingDebt }
    val totalNextDueAllCards = summaries.sumOf { it.estimatedNextPaymentDue }
    val totalLimitAllCards = summaries.sumOf { it.card.creditLimit }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingCard = null
                    showAddEditCardDialog = true
                },
                icon = { Icon(Icons.Default.AddCard, contentDescription = "Add Credit Card") },
                text = { Text("Add Card") },
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
            contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp)
        ) {
            item {
                Text(
                    text = "Credit Cards & Loans",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Overall Credit Debt Summary Banner
            item {
                GradientCard {
                    Text(
                        text = "Total Credit Card Debt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatCurrency(totalDebtAllCards, settings.currencySymbol),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Due Next Cycle", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                Formatters.formatCurrency(totalNextDueAllCards, settings.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MintLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Available Limit", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                Formatters.formatCurrency((totalLimitAllCards - totalDebtAllCards).coerceAtLeast(0.0), settings.currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (summaries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Credit Cards Added Yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add your credit card (including any previous unpaid balance) to track card purchases, cash loans, next payment dates, and bill adjustments.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddEditCardDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Your First Credit Card")
                            }
                        }
                    }
                }
            } else {
                items(summaries, key = { it.card.id }) { summary ->
                    CreditCardDetailItem(
                        summary = summary,
                        currencySymbol = settings.currencySymbol,
                        onBuyClick = { purchaseCard = summary.card },
                        onLoanClick = { loanCard = summary.card },
                        onPayBillClick = { paymentCardSummary = summary },
                        onEditCard = {
                            editingCard = summary.card
                            showAddEditCardDialog = true
                        },
                        onDeleteCard = { deletingCard = summary.card },
                        onDeleteTx = { txId -> viewModel.deleteCardTransaction(txId) }
                    )
                }
            }
        }
    }

    // 1. Add / Edit Credit Card Dialog
    if (showAddEditCardDialog) {
        AddEditCreditCardDialog(
            initial = editingCard,
            onDismiss = {
                showAddEditCardDialog = false
                editingCard = null
            },
            onSave = { card ->
                viewModel.saveCard(card)
                showAddEditCardDialog = false
                editingCard = null
            }
        )
    }

    // 2. Delete Credit Card Confirmation
    if (deletingCard != null) {
        AlertDialog(
            onDismissRequest = { deletingCard = null },
            title = { Text("Delete ${deletingCard?.cardName}?") },
            text = { Text("This will remove the credit card from your list. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCard(deletingCard!!.id)
                        deletingCard = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCard = null }) { Text("Cancel") }
            }
        )
    }

    // 3. Record Card Purchase Dialog
    if (purchaseCard != null) {
        CardPurchaseDialog(
            card = purchaseCard!!,
            categories = categories,
            onDismiss = { purchaseCard = null },
            onConfirm = { amount, catId, catName, note ->
                viewModel.recordCardPurchase(purchaseCard!!, amount, catId, catName, note)
                purchaseCard = null
            }
        )
    }

    // 4. Take Loan from Credit Card Dialog
    if (loanCard != null) {
        CardLoanDialog(
            card = loanCard!!,
            onDismiss = { loanCard = null },
            onConfirm = { principal, fee, emiMonths, depositTo, note ->
                viewModel.recordCardLoan(loanCard!!, principal, fee, emiMonths, depositTo, note)
                loanCard = null
            }
        )
    }

    // 5. Pay Credit Card Bill Dialog
    if (paymentCardSummary != null) {
        PayCardBillDialog(
            summary = paymentCardSummary!!,
            currencySymbol = settings.currencySymbol,
            onDismiss = { paymentCardSummary = null },
            onConfirm = { amount, source, note ->
                viewModel.payCardBill(paymentCardSummary!!.card, amount, source, note)
                paymentCardSummary = null
            }
        )
    }
}

@Composable
fun CreditCardDetailItem(
    summary: CardFinancialSummary,
    currencySymbol: String,
    onBuyClick: () -> Unit,
    onLoanClick: () -> Unit,
    onPayBillClick: () -> Unit,
    onEditCard: () -> Unit,
    onDeleteCard: () -> Unit,
    onDeleteTx: (String) -> Unit
) {
    val card = summary.card
    var showHistory by remember { mutableStateOf(false) }

    val dueBadgeColor = when {
        summary.totalOutstandingDebt <= 0.0 -> GreenSuccess
        summary.daysUntilDue <= 3 -> RedError
        summary.daysUntilDue <= 7 -> OrangeWarning
        else -> BlueInfo
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Metallic-style Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MintLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${card.bankName} • ${card.cardName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (card.lastFourDigits.isNotBlank()) {
                                    Text(
                                        text = "•••• ${card.lastFourDigits}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = onEditCard, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Card", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDeleteCard, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Card", tint = RedError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Current Total Debt", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                text = Formatters.formatCurrency(summary.totalOutstandingDebt, currencySymbol),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.totalOutstandingDebt > 0) Color(0xFFFCA5A5) else MintLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Available Credit", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                text = Formatters.formatCurrency(summary.availableCredit, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (summary.utilizationPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (summary.utilizationPercent > 80f) RedError else MintAccent,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Credit Limit: ${Formatters.formatCurrency(card.creditLimit, currencySymbol)} (${summary.utilizationPercent.toInt()}% used)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Payment Planner & Due Date Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = dueBadgeColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = dueBadgeColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Next Payment Date: ${summary.nextPaymentDateText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.totalOutstandingDebt <= 0.0) {
                                    "All clear! No payment needed."
                                } else {
                                    "Min Due: ${Formatters.formatCurrency(summary.minimumPaymentDue, currencySymbol)} • (${summary.daysUntilDue} days left)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Amount to Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatCurrency(summary.estimatedNextPaymentDue, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = dueBadgeColor
                            )
                        }
                    }
                }

                // Debt Breakdown Row (Purchases vs Loans vs Monthly EMI)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Month Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(summary.currentMonthPurchases, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text("Loans Taken", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(summary.activeLoansTotalRemaining, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Monthly Loan EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(summary.monthlyLoanEmiTotal, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 3 Action Buttons: Buy Stuff, Take Loan, Pay Bill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBuyClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buy Stuff", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onLoanClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.RequestQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Loan", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onPayBillClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Bill", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Toggleable Card Transaction History
                if (summary.cardTransactions.isNotEmpty() || card.previousDebt > 0.0) {
                    TextButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showHistory) "Hide Card History ▲" else "View Card Activity & Loans (${summary.cardTransactions.size}) ▼")
                    }

                    AnimatedVisibility(visible = showHistory) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (card.previousDebt > 0.0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Initial / Previous Debt Carryover", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        Formatters.formatCurrency(card.previousDebt, currencySymbol),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeWarning
                                    )
                                }
                            }
                            summary.cardTransactions.take(15).forEach { tx ->
                                val isPayment = tx.isCardBillPayment || tx.type == TransactionType.CARD_PAYMENT.name
                                val isLoan = tx.type == TransactionType.CARD_LOAN.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.note.ifBlank { tx.categoryName },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val subText = when {
                                            isPayment -> "Bill Paid via ${tx.paymentSource} • ${Formatters.formatDate(tx.dateMillis)}"
                                            isLoan -> "Card Loan (${if (tx.emiMonths > 1) "${tx.emiMonths}m EMI" else "One-time"}) • ${Formatters.formatDate(tx.dateMillis)}"
                                            else -> "${tx.categoryName} • ${Formatters.formatDate(tx.dateMillis)}"
                                        }
                                        Text(subText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${if (isPayment) "-" else "+"}${Formatters.formatCurrency(tx.amount, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPayment) GreenSuccess else RedError
                                        )
                                        IconButton(onClick = { onDeleteTx(tx.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete entry", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCreditCardDialog(
    initial: CreditCard?,
    onDismiss: () -> Unit,
    onSave: (CreditCard) -> Unit
) {
    var bankName by remember { mutableStateOf(initial?.bankName ?: "") }
    var cardName by remember { mutableStateOf(initial?.cardName ?: "") }
    var lastFour by remember { mutableStateOf(initial?.lastFourDigits ?: "") }
    var creditLimit by remember { mutableStateOf(initial?.creditLimit?.takeIf { it > 0 }?.toString() ?: "") }
    var prevDebt by remember { mutableStateOf(initial?.previousDebt?.takeIf { it > 0 }?.toString() ?: "") }
    var statementDay by remember { mutableStateOf(initial?.statementDay?.toString() ?: "15") }
    var paymentDueDay by remember { mutableStateOf(initial?.paymentDueDay?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Credit Card" else "Edit Credit Card") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g., City Bank, EBL, BRAC)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name (e.g., Amex, Visa Signature)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastFour,
                    onValueChange = { if (it.length <= 4) lastFour = it },
                    label = { Text("Last 4 Digits (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("Total Credit Limit (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prevDebt,
                    onValueChange = { prevDebt = it },
                    label = { Text("Existing / Previous Unpaid Debt (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = statementDay,
                        onValueChange = { statementDay = it },
                        label = { Text("Statement Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentDueDay,
                        onValueChange = { paymentDueDay = it },
                        label = { Text("Payment Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (cardName.isNotBlank() || bankName.isNotBlank()) {
                    onSave(
                        (initial ?: CreditCard()).copy(
                            bankName = bankName.trim().ifBlank { "Bank" },
                            cardName = cardName.trim().ifBlank { "Credit Card" },
                            lastFourDigits = lastFour.trim(),
                            creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                            previousDebt = prevDebt.toDoubleOrNull() ?: 0.0,
                            statementDay = (statementDay.toIntOrNull() ?: 15).coerceIn(1, 31),
                            paymentDueDay = (paymentDueDay.toIntOrNull() ?: 5).coerceIn(1, 31)
                        )
                    )
                }
            }) {
                Text("Save Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CardPurchaseDialog(
    card: CreditCard,
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "shopping") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buy with ${card.cardName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Adds to your Credit Card debt and monthly spending without deducting from your Bank Balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Purchase Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("What did you buy? (e.g., Dinner, Gadget, Grocery)") },
                    singleLine = true
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0.0) {
                    val catName = categories.find { it.id == selectedCatId }?.name ?: "Shopping"
                    onConfirm(amt, selectedCatId, catName, note)
                }
            }) {
                Text("Record Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CardLoanDialog(
    card: CreditCard,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Int, String, String) -> Unit
) {
    var principal by remember { mutableStateOf("") }
    var feeOrInterest by remember { mutableStateOf("") }
    var emiMonths by remember { mutableStateOf("1") }
    var depositTo by remember { mutableStateOf("BANK") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take Loan from ${card.cardName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Loan Principal Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = feeOrInterest,
                    onValueChange = { feeOrInterest = it },
                    label = { Text("Interest / Processing Fee (৳ Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = emiMonths,
                    onValueChange = { emiMonths = it },
                    label = { Text("Repayment Months / EMI Tenure (1 = Next Bill)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Loan Purpose / Note") },
                    singleLine = true
                )
                Text("Where did the loan money go?", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BANK" to "To Bank", "CASH" to "Cash in Hand", "NONE" to "Don't Add").forEach { (key, label) ->
                        FilterChip(
                            selected = depositTo == key,
                            onClick = { depositTo = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = principal.toDoubleOrNull() ?: 0.0
                val f = feeOrInterest.toDoubleOrNull() ?: 0.0
                val m = (emiMonths.toIntOrNull() ?: 1).coerceAtLeast(1)
                if (p > 0.0) {
                    onConfirm(p, f, m, depositTo, note)
                }
            }) {
                Text("Add Card Loan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PayCardBillDialog(
    summary: CardFinancialSummary,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf(summary.estimatedNextPaymentDue.takeIf { it > 0 }?.toString() ?: "") }
    var source by remember { mutableStateOf(PaymentSource.BANK.name) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${summary.card.cardName} Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Total Debt: ${Formatters.formatCurrency(summary.totalOutstandingDebt, currencySymbol)}\nNext Due: ${Formatters.formatCurrency(summary.estimatedNextPaymentDue, currencySymbol)} (Min: ${Formatters.formatCurrency(summary.minimumPaymentDue, currencySymbol)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { amount = summary.estimatedNextPaymentDue.toString() },
                        label = { Text("Next Due") }
                    )
                    AssistChip(
                        onClick = { amount = summary.minimumPaymentDue.toString() },
                        label = { Text("Min Due") }
                    )
                    AssistChip(
                        onClick = { amount = summary.totalOutstandingDebt.toString() },
                        label = { Text("Full Debt") }
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Text("Pay From Account", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(PaymentSource.BANK, PaymentSource.CASH, PaymentSource.BKASH, PaymentSource.NAGAD)) { src ->
                        FilterChip(
                            selected = source == src.name,
                            onClick = { source = src.name },
                            label = { Text(src.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(amt, source, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("Confirm Bill Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}