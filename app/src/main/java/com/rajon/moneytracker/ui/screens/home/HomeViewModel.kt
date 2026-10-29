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

    val monthSetup: StateFlow<MonthSetup?> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeMonthSetup(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val debtsList: StateFlow<List<DebtItem>> = financeRepository.observeDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceBreakdown: StateFlow<BalanceBreakdown> = combine(
        monthSetup, transactions, savingsDeposits, debtPayments
    ) { setup, txs, deps, pays ->
        BalanceCalculator.calculate(setup, txs, deps, pays)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BalanceBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
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

    fun recordQuickWithdrawal(amount: Double) {
        viewModelScope.launch {
            val tx = Transaction(
                yearMonth = _currentYearMonth.value,
                type = TransactionType.WITHDRAWAL.name,
                amount = amount,
                dateMillis = System.currentTimeMillis(),
                paymentSource = PaymentSource.BANK.name,
                note = "ATM Withdrawal"
            )
            financeRepository.saveTransaction(tx)
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
