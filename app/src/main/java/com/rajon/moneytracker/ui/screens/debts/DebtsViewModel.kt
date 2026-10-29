package com.rajon.moneytracker.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.DebtItem
import com.rajon.moneytracker.data.model.DebtPaymentRecord
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DebtsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val debts: StateFlow<List<DebtItem>> = financeRepository.observeDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDebt(debt: DebtItem) {
        viewModelScope.launch {
            financeRepository.saveDebt(debt.copy(createdInYearMonth = DateUtils.getCurrentYearMonth()))
        }
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            financeRepository.deleteDebt(debtId)
        }
    }

    fun addPayment(debt: DebtItem, amount: Double, paymentSource: String, note: String, createExpense: Boolean) {
        viewModelScope.launch {
            val payment = DebtPaymentRecord(
                debtId = debt.id,
                debtType = debt.type,
                yearMonth = DateUtils.getCurrentYearMonth(),
                amount = amount,
                dateMillis = System.currentTimeMillis(),
                paymentSource = paymentSource,
                note = note
            )
            financeRepository.addDebtPayment(debt, payment, createExpense)
        }
    }
}
