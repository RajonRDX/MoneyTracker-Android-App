package com.rajon.moneytracker.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.rajon.moneytracker.data.di.AppContainer
import com.rajon.moneytracker.ui.screens.auth.AuthScreen
import com.rajon.moneytracker.ui.screens.auth.AuthViewModel
import com.rajon.moneytracker.ui.screens.home.HomeScreen
import com.rajon.moneytracker.ui.screens.home.HomeViewModel
import com.rajon.moneytracker.ui.screens.transactions.TransactionsScreen
import com.rajon.moneytracker.ui.screens.transactions.TransactionsViewModel
import com.rajon.moneytracker.ui.screens.budget.BudgetScreen
import com.rajon.moneytracker.ui.screens.budget.BudgetViewModel
import com.rajon.moneytracker.ui.screens.savings.SavingsScreen
import com.rajon.moneytracker.ui.screens.savings.SavingsViewModel
import com.rajon.moneytracker.ui.screens.debts.DebtsScreen
import com.rajon.moneytracker.ui.screens.debts.DebtsViewModel
import com.rajon.moneytracker.ui.screens.history.HistoryScreen
import com.rajon.moneytracker.ui.screens.history.HistoryViewModel
import com.rajon.moneytracker.ui.screens.more.SettingsScreen
import com.rajon.moneytracker.ui.screens.more.SettingsViewModel

@Composable
fun AppNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.AUTH
    ) {
        composable(NavRoutes.AUTH) {
            AuthScreen(
                viewModel = AuthViewModel(appContainer.authRepository),
                onLoginSuccess = { isLoggedIn = true }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = HomeViewModel(appContainer.financeRepository),
                onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                onNavigateToBudget = { navController.navigate(NavRoutes.BUDGET) }
            )
        }
        composable(NavRoutes.TRANSACTIONS) {
            TransactionsScreen(
                viewModel = TransactionsViewModel(appContainer.financeRepository, appContainer.localPrefs)
            )
        }
        composable(NavRoutes.BUDGET) {
            BudgetScreen(
                viewModel = BudgetViewModel(appContainer.financeRepository)
            )
        }
        composable(NavRoutes.SAVINGS) {
            SavingsScreen(
                viewModel = SavingsViewModel(appContainer.financeRepository)
            )
        }
        composable(NavRoutes.DEBTS) {
            DebtsScreen(
                viewModel = DebtsViewModel(appContainer.financeRepository),
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                viewModel = HistoryViewModel(appContainer.financeRepository),
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = SettingsViewModel(appContainer.financeRepository, appContainer.localPrefs),
                onBack = { navController.popBackStack() }
            )
        }
    }
}