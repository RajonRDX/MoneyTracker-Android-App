package com.rajon.moneytracker.data.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.rajon.moneytracker.MoneyTrackerApp
import com.rajon.moneytracker.ui.screens.auth.AuthViewModel
import com.rajon.moneytracker.ui.screens.budget.BudgetViewModel
import com.rajon.moneytracker.ui.screens.debts.DebtsViewModel
import com.rajon.moneytracker.ui.screens.home.HomeViewModel
import com.rajon.moneytracker.ui.screens.history.HistoryViewModel
import com.rajon.moneytracker.ui.screens.savings.SavingsViewModel
import com.rajon.moneytracker.ui.screens.transactions.TransactionsViewModel
import com.rajon.moneytracker.ui.screens.more.SettingsViewModel

object AppViewModelProvider {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[APPLICATION_KEY]) as MoneyTrackerApp
            val container = application.container

            return when {
                modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                    AuthViewModel(container.authRepository) as T
                modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                    HomeViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                    TransactionsViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(BudgetViewModel::class.java) ->
                    BudgetViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(SavingsViewModel::class.java) ->
                    SavingsViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(DebtsViewModel::class.java) ->
                    DebtsViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                    HistoryViewModel(container.financeRepository) as T
                modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                    SettingsViewModel(container.financeRepository, container.localPrefs) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}