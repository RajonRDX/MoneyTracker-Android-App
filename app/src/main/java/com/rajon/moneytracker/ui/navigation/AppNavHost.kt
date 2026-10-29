package com.rajon.moneytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rajon.moneytracker.data.di.AppContainer
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.ui.screens.auth.AuthScreen
import com.rajon.moneytracker.ui.screens.auth.AuthViewModel
import com.rajon.moneytracker.ui.screens.budget.BudgetScreen
import com.rajon.moneytracker.ui.screens.debts.DebtsScreen
import com.rajon.moneytracker.ui.screens.history.HistoryScreen
import com.rajon.moneytracker.ui.screens.home.HomeScreen
import com.rajon.moneytracker.ui.screens.more.MoreScreen
import com.rajon.moneytracker.ui.screens.more.SettingsScreen
import com.rajon.moneytracker.ui.screens.savings.SavingsScreen
import com.rajon.moneytracker.ui.screens.transactions.TransactionsScreen

@Composable
fun AppNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val authRepo = appContainer.authRepository
    val isLoggedIn by authRepo.currentUserIdFlow.collectAsStateWithLifecycle(initialValue = authRepo.isLoggedIn)

    val startDest = if (isLoggedIn) NavRoutes.HOME else NavRoutes.AUTH

    val items = listOf(
        Triple(NavRoutes.HOME, "Home", Icons.Default.Home),
        Triple(NavRoutes.TRANSACTIONS, "Transactions", Icons.Default.Receipt),
        Triple(NavRoutes.BUDGET, "Budget", Icons.Default.PieChart),
        Triple(NavRoutes.SAVINGS, "Savings", Icons.Default.Savings),
        Triple(NavRoutes.MORE, "More", Icons.Default.Menu)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute != null && currentRoute != NavRoutes.AUTH && currentRoute != NavRoutes.DEBTS && currentRoute != NavRoutes.HISTORY && currentRoute != NavRoutes.SETTINGS) {
                NavigationBar {
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.AUTH) {
                val vm: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AuthScreen(
                    viewModel = vm,
                    onLoginSuccess = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                    onNavigateToBudget = { navController.navigate(NavRoutes.BUDGET) }
                )
            }
            composable(NavRoutes.TRANSACTIONS) {
                TransactionsScreen()
            }
            composable(NavRoutes.BUDGET) {
                BudgetScreen()
            }
            composable(NavRoutes.SAVINGS) {
                SavingsScreen()
            }
            composable(NavRoutes.MORE) {
                MoreScreen(
                    onNavigateToDebts = { navController.navigate(NavRoutes.DEBTS) },
                    onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onLogout = {
                        appContainer.authRepository.logout()
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.DEBTS) {
                DebtsScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.HISTORY) {
                HistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}