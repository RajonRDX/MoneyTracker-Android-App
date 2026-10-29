package com.rajon.moneytracker.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.*
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val currentYearMonth: StateFlow<String> = _currentYearMonth.asStateFlow()

    val categories: StateFlow<List<ExpenseCategory>> = financeRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = _currentYearMonth.flatMapLatest { ym ->
        financeRepository.observeTransactions(ym)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeMonth(yearMonth: String) {
        _currentYearMonth.value = yearMonth
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
}
