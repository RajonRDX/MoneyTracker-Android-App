package com.rajon.moneytracker.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.rajon.moneytracker.data.di.AppContainer
import com.rajon.moneytracker.ui.screens.auth.AuthScreen
import com.rajon.moneytracker.ui.screens.home.HomeScreen
import com.rajon.moneytracker.ui.screens.transactions.TransactionsScreen
import com.rajon.moneytracker.ui.screens.budget.BudgetScreen
import com.rajon.moneytracker.ui.screens.savings.SavingsScreen
import com.rajon.moneytracker.ui.screens.debts.DebtsScreen
import com.rajon.moneytracker.ui.screens.history.HistoryScreen
import com.rajon.moneytracker.ui.screens.more.SettingsScreen

@Composable
fun AppNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.LOGIN
    ) {
        composable(NavRoutes.LOGIN) {
            AuthScreen(
                authRepository = appContainer.authRepository,
                onAuthSuccess = { isLoggedIn = true }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.TRANSACTIONS) {
            TransactionsScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.BUDGET) {
            BudgetScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.SAVINGS) {
            SavingsScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.DEBTS) {
            DebtsScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                firestoreRepository = appContainer.financeRepository,
                navController = navController
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                authRepository = appContainer.authRepository,
                firestoreRepository = appContainer.financeRepository,
                localPrefs = appContainer.localPrefs,
                navController = navController,
                onLogout = { isLoggedIn = false }
            )
        }
    }
}