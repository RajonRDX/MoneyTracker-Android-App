package com.rajon.moneytracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.*
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.domain.BalanceBreakdown
import com.rajon.moneytracker.domain.BalanceCalculator
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val currentYearMonth: StateFlow<String> = _currentYearMonth.asStateFlow()

    val settings: StateFlow<UserSettings> = financeRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val categories: StateFlow<List<ExpenseCategory>> = financeRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSetup: StateFlow<MonthSetup?> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeMonthSetup(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allMonthSetups: StateFlow<List<MonthSetup>> = financeRepository.observeAllMonthSetups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions: StateFlow<List<Transaction>> = financeRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeTransactions(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsDeposits: StateFlow<List<SavingsDeposit>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeMonthDeposits(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtPayments: StateFlow<List<DebtPaymentRecord>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeMonthDebtPayments(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringList: StateFlow<List<RecurringPayment>> = financeRepository.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceBreakdown: StateFlow<BalanceBreakdown> = combine(
        _currentYearMonth,
        monthSetup,
        transactions,
        savingsDeposits,
        debtPayments,
        allMonthSetups,
        allTransactions
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val ym = args[0] as String
        val setup = args[1] as MonthSetup?
        val txs = args[2] as List<Transaction>
        val deps = args[3] as List<SavingsDeposit>
        val pays = args[4] as List<DebtPaymentRecord>
        val allSetups = args[5] as List<MonthSetup>
        val allTxs = args[6] as List<Transaction>

        // Find the most recent previous month setup or previous month activity
        val prevYM = DateUtils.getPreviousYearMonth(ym)
        val latestPrevSetup = allSetups
            .filter { it.yearMonth < ym }
            .maxByOrNull { it.yearMonth }

        val prevMonthTxs = allTxs.filter { it.yearMonth == prevYM }
        val prevBreakdown = if (latestPrevSetup != null || prevMonthTxs.isNotEmpty()) {
            BalanceCalculator.calculate(
                monthSetup = allSetups.find { it.yearMonth == prevYM } ?: latestPrevSetup,
                transactions = prevMonthTxs,
                savingsDeposits = emptyList(),
                debtPayments = emptyList()
            )
        } else null

        // Check if salary was added at the end of previous month (day >= 25) or in previous setup
        val latePrevMonthSalaryTx = prevMonthTxs
            .filter { it.type == TransactionType.INCOME.name && DateUtils.getDayOfMonth(it.dateMillis) >= 24 }
            .sumOf { it.amount }

        val fallbackSalary = when {
            latePrevMonthSalaryTx > 0.0 -> latePrevMonthSalaryTx
            latestPrevSetup != null && latestPrevSetup.salary > 0.0 -> latestPrevSetup.salary
            else -> 0.0
        }

        val hasThisMonthIncomeTx = txs.any { it.type == TransactionType.INCOME.name }

        if (setup == null) {
            // Automatic rollover when new month hasn't been manually configured yet
            val rolledOverBank = prevBreakdown?.bankBalanceLeft?.coerceAtLeast(0.0) ?: 0.0
            val rolledOverCash = prevBreakdown?.cashInHand?.coerceAtLeast(0.0) ?: 0.0
            val rolledOverSalary = if (hasThisMonthIncomeTx) 0.0 else if (rolledOverBank > 0.0 && latePrevMonthSalaryTx > 0.0) 0.0 else fallbackSalary

            BalanceCalculator.calculate(
                monthSetup = null,
                transactions = txs,
                savingsDeposits = deps,
                debtPayments = pays,
                carriedOverBankBalance = rolledOverBank,
                carriedOverCashBalance = rolledOverCash,
                carriedOverSalary = rolledOverSalary,
                isCarriedOver = (rolledOverBank > 0.0 || rolledOverCash > 0.0 || rolledOverSalary > 0.0)
            )
        } else {
            // Even if setup exists, if salary & opening bank are both 0 and no income tx added yet, carry over previous salary/balance
            val effectiveSetup = if (setup.salary == 0.0 && setup.openingBankBalance == 0.0 && !hasThisMonthIncomeTx) {
                setup.copy(
                    openingBankBalance = prevBreakdown?.bankBalanceLeft?.coerceAtLeast(0.0) ?: 0.0,
                    openingBalance = prevBreakdown?.cashInHand?.coerceAtLeast(0.0) ?: 0.0,
                    salary = if ((prevBreakdown?.bankBalanceLeft ?: 0.0) > 0.0 && latePrevMonthSalaryTx > 0.0) 0.0 else fallbackSalary
                )
            } else {
                setup
            }
            BalanceCalculator.calculate(
                monthSetup = effectiveSetup,
                transactions = txs,
                savingsDeposits = deps,
                debtPayments = pays,
                isCarriedOver = effectiveSetup != setup
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BalanceBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    init {
        viewModelScope.launch {
            financeRepository.ensureDefaultCategories()
        }
    }

    fun changeMonth(yearMonth: String) {
        _currentYearMonth.value = yearMonth
    }

    fun saveMonthSetup(setup: MonthSetup) {
        viewModelScope.launch {
            financeRepository.saveMonthSetup(setup.copy(yearMonth = _currentYearMonth.value))
        }
    }

    fun saveTransaction(tx: Transaction) {
        viewModelScope.launch {
            val finalTx = if (tx.yearMonth.isBlank()) tx.copy(yearMonth = _currentYearMonth.value) else tx
            financeRepository.saveTransaction(finalTx)
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(txId)
        }
    }

    fun markRecurringPaid(item: RecurringPayment) {
        viewModelScope.launch {
            financeRepository.markRecurringPaid(item, _currentYearMonth.value, System.currentTimeMillis())
        }
    }

    fun markRecurringSkipped(item: RecurringPayment) {
        viewModelScope.launch {
            financeRepository.markRecurringSkipped(item, _currentYearMonth.value)
        }
    }
}