package com.rajon.moneytracker.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.*
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.domain.BudgetEvaluator
import com.rajon.moneytracker.domain.BudgetStatus
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val currentYearMonth: StateFlow<String> = _currentYearMonth.asStateFlow()

    val categories: StateFlow<List<ExpenseCategory>> = financeRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudget>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeBudgets(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeTransactions(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSetup: StateFlow<MonthSetup?> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeMonthSetup(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val budgetStatuses: StateFlow<List<BudgetStatus>> = combine(
        budgets, transactions, monthSetup
    ) { bList, txs, setup ->
        BudgetEvaluator.evaluateBudgets(
            budgets = bList,
            transactions = txs,
            overallLimit = setup?.overallBudgetLimit ?: 0.0,
            extraLimit = setup?.extraSpendingLimit ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeMonth(yearMonth: String) {
        _currentYearMonth.value = yearMonth
    }

    fun saveBudget(categoryId: String, categoryName: String, limitAmount: Double) {
        viewModelScope.launch {
            val budget = CategoryBudget(
                id = "${_currentYearMonth.value}_$categoryId",
                yearMonth = _currentYearMonth.value,
                categoryId = categoryId,
                categoryName = categoryName,
                limitAmount = limitAmount
            )
            financeRepository.saveBudget(budget)
        }
    }
}
