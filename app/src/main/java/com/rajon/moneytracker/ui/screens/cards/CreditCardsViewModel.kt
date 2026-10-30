package com.rajon.moneytracker.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.*
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class CardFinancialSummary(
    val card: CreditCard,
    val totalOutstandingDebt: Double,
    val availableCredit: Double,
    val utilizationPercent: Float,
    val currentMonthPurchases: Double,
    val activeLoansTotalRemaining: Double,
    val monthlyLoanEmiTotal: Double,
    val estimatedNextPaymentDue: Double,
    val minimumPaymentDue: Double,
    val nextPaymentDateText: String,
    val daysUntilDue: Int,
    val cardTransactions: List<Transaction>
)

class CreditCardsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val settings: StateFlow<UserSettings> = financeRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val cards: StateFlow<List<CreditCard>> = financeRepository.observeCreditCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<Transaction>> = financeRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<ExpenseCategory>> = financeRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cardSummaries: StateFlow<List<CardFinancialSummary>> = combine(
        cards,
        allTransactions
    ) { cardList, txs ->
        val currentYM = DateUtils.getCurrentYearMonth()

        cardList.map { card ->
            val cardTxs = txs.filter { tx ->
                tx.linkedCardId == card.id || (tx.linkedCardId == null && tx.paymentSource == PaymentSource.CARD.name && cardList.size == 1)
            }.sortedByDescending { it.dateMillis }

            val totalPurchases = cardTxs
                .filter { it.type == TransactionType.EXPENSE.name && !it.isCardBillPayment }
                .sumOf { it.amount }

            val totalLoansTaken = cardTxs
                .filter { it.type == TransactionType.CARD_LOAN.name }
                .sumOf { it.amount }

            val totalPaymentsMade = cardTxs
                .filter { it.isCardBillPayment || it.type == TransactionType.CARD_PAYMENT.name }
                .sumOf { it.amount }

            val totalDebt = (card.previousDebt + totalPurchases + totalLoansTaken - totalPaymentsMade).coerceAtLeast(0.0)
            val available = (card.creditLimit - totalDebt).coerceAtLeast(0.0)
            val utilPct = if (card.creditLimit > 0.0) ((totalDebt / card.creditLimit) * 100f).toFloat().coerceIn(0f, 100f) else 0f

            val thisMonthPurchases = cardTxs
                .filter { it.yearMonth == currentYM && it.type == TransactionType.EXPENSE.name && !it.isCardBillPayment }
                .sumOf { it.amount }

            val monthlyEmi = cardTxs
                .filter { it.type == TransactionType.CARD_LOAN.name && it.emiMonths > 0 }
                .sumOf { it.amount / it.emiMonths }

            val nonEmiLoans = cardTxs
                .filter { it.type == TransactionType.CARD_LOAN.name && it.emiMonths <= 1 }
                .sumOf { it.amount }

            val thisMonthPayments = cardTxs
                .filter { it.yearMonth == currentYM && (it.isCardBillPayment || it.type == TransactionType.CARD_PAYMENT.name) }
                .sumOf { it.amount }

            val rawStatementDue = if (monthlyEmi > 0.0) {
                (card.previousDebt + thisMonthPurchases + monthlyEmi + nonEmiLoans - thisMonthPayments).coerceIn(0.0, totalDebt)
            } else {
                totalDebt
            }
            val estimatedDue = if (rawStatementDue > 0.0) rawStatementDue else totalDebt

            val minDue = if (totalDebt > 0.0) {
                (totalDebt * (card.minimumDuePercent / 100.0)).coerceAtLeast(500.0).coerceAtMost(totalDebt)
            } else 0.0

            val (nextDateStr, daysLeft) = calculateNextPaymentDate(card.paymentDueDay)

            CardFinancialSummary(
                card = card,
                totalOutstandingDebt = totalDebt,
                availableCredit = available,
                utilizationPercent = utilPct,
                currentMonthPurchases = thisMonthPurchases,
                activeLoansTotalRemaining = totalLoansTaken.coerceAtMost(totalDebt),
                monthlyLoanEmiTotal = monthlyEmi,
                estimatedNextPaymentDue = estimatedDue,
                minimumPaymentDue = minDue,
                nextPaymentDateText = nextDateStr,
                daysUntilDue = daysLeft,
                cardTransactions = cardTxs
            )
        }
    }.onEach {
        _isLoading.value = false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCard(card: CreditCard) {
        viewModelScope.launch {
            financeRepository.saveCreditCard(card)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            financeRepository.deleteCreditCard(cardId)
        }
    }

    fun recordCardPurchase(card: CreditCard, amount: Double, categoryId: String, categoryName: String, note: String) {
        viewModelScope.launch {
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                yearMonth = DateUtils.getCurrentYearMonth(),
                type = TransactionType.EXPENSE.name,
                amount = amount,
                dateMillis = System.currentTimeMillis(),
                categoryId = categoryId,
                categoryName = categoryName,
                paymentSource = PaymentSource.CARD.name,
                linkedCardId = card.id,
                linkedCardName = "${card.bankName} ${card.cardName}".trim(),
                note = note.ifBlank { "Card Purchase (${card.cardName})" },
                createdAt = System.currentTimeMillis()
            )
            financeRepository.saveTransaction(tx)
        }
    }

    fun recordCardLoan(card: CreditCard, principalAmount: Double, processingFeeOrInterest: Double, emiMonths: Int, depositToSource: String, note: String) {
        viewModelScope.launch {
            val totalLoanLiability = principalAmount + processingFeeOrInterest
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                yearMonth = DateUtils.getCurrentYearMonth(),
                type = TransactionType.CARD_LOAN.name,
                amount = totalLoanLiability,
                cardLoanPrincipal = principalAmount,
                dateMillis = System.currentTimeMillis(),
                categoryId = "card_loan",
                categoryName = "Credit Card Loan",
                paymentSource = depositToSource,
                linkedCardId = card.id,
                linkedCardName = "${card.bankName} ${card.cardName}".trim(),
                emiMonths = emiMonths,
                note = note.ifBlank { "Loan from ${card.cardName}" },
                createdAt = System.currentTimeMillis()
            )
            financeRepository.saveTransaction(tx)
        }
    }

    fun payCardBill(card: CreditCard, amount: Double, paidFromSource: String, note: String) {
        viewModelScope.launch {
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                yearMonth = DateUtils.getCurrentYearMonth(),
                type = TransactionType.CARD_PAYMENT.name,
                amount = amount,
                dateMillis = System.currentTimeMillis(),
                categoryId = "card_bill",
                categoryName = "Credit Card Bill Payment",
                paymentSource = paidFromSource,
                linkedCardId = card.id,
                linkedCardName = "${card.bankName} ${card.cardName}".trim(),
                isCardBillPayment = true,
                note = note.ifBlank { "Bill Payment - ${card.cardName}" },
                createdAt = System.currentTimeMillis()
            )
            financeRepository.saveTransaction(tx)
        }
    }

    fun deleteCardTransaction(txId: String) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(txId)
        }
    }

    private fun calculateNextPaymentDate(dueDay: Int): Pair<String, Int> {
        val now = Calendar.getInstance()
        val todayDay = now.get(Calendar.DAY_OF_MONTH)

        val dueCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val maxDayThisMonth = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val clampedThisMonth = dueDay.coerceIn(1, maxDayThisMonth)

        if (todayDay <= clampedThisMonth) {
            dueCal.set(Calendar.DAY_OF_MONTH, clampedThisMonth)
        } else {
            dueCal.add(Calendar.MONTH, 1)
            val maxDayNextMonth = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dueCal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, maxDayNextMonth))
        }

        val diffMillis = (dueCal.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        val daysLeft = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return Pair(sdf.format(dueCal.time), daysLeft)
    }
}