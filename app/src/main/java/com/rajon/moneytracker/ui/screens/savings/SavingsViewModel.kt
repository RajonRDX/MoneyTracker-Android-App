package com.rajon.moneytracker.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.SavingsDeposit
import com.rajon.moneytracker.data.model.SavingsGoal
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavingsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = financeRepository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            financeRepository.saveGoal(goal)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            financeRepository.deleteGoal(goalId)
        }
    }

    fun addDeposit(goal: SavingsGoal, amount: Double, isWithdrawal: Boolean, takeFromBank: Boolean, note: String) {
        viewModelScope.launch {
            val deposit = SavingsDeposit(
                goalId = goal.id,
                yearMonth = DateUtils.getCurrentYearMonth(),
                amount = amount,
                isWithdrawal = isWithdrawal,
                takeFromBank = takeFromBank,
                dateMillis = System.currentTimeMillis(),
                note = note
            )
            financeRepository.addGoalDeposit(goal, deposit)
        }
    }
}
