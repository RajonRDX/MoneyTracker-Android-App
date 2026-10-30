package com.rajon.moneytracker.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.rajon.moneytracker.data.di.AppContainer
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.ui.screens.auth.AuthScreen
import com.rajon.moneytracker.ui.screens.auth.AuthViewModel
import com.rajon.moneytracker.ui.screens.budget.BudgetScreen
import com.rajon.moneytracker.ui.screens.budget.BudgetViewModel
import com.rajon.moneytracker.ui.screens.cards.CreditCardsScreen
import com.rajon.moneytracker.ui.screens.cards.CreditCardsViewModel
import com.rajon.moneytracker.ui.screens.debts.DebtsScreen
import com.rajon.moneytracker.ui.screens.debts.DebtsViewModel
import com.rajon.moneytracker.ui.screens.history.HistoryScreen
import com.rajon.moneytracker.ui.screens.history.HistoryViewModel
import com.rajon.moneytracker.ui.screens.home.HomeScreen
import com.rajon.moneytracker.ui.screens.home.HomeViewModel
import com.rajon.moneytracker.ui.screens.more.MoreScreen
import com.rajon.moneytracker.ui.screens.more.SettingsScreen
import com.rajon.moneytracker.ui.screens.more.SettingsViewModel
import com.rajon.moneytracker.ui.screens.savings.SavingsScreen
import com.rajon.moneytracker.ui.screens.savings.SavingsViewModel
import com.rajon.moneytracker.ui.screens.transactions.TransactionsScreen
import com.rajon.moneytracker.ui.screens.transactions.TransactionsViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(NavRoutes.HOME, "Home", Icons.Default.Home)
    object Transactions : BottomNavItem(NavRoutes.TRANSACTIONS, "Transactions", Icons.Default.ReceiptLong)
    object Cards : BottomNavItem(NavRoutes.CARDS, "Credit Card", Icons.Default.CreditCard)
    object Budget : BottomNavItem(NavRoutes.BUDGET, "Budget", Icons.Default.Assessment)
    object More : BottomNavItem(NavRoutes.MORE, "More", Icons.Default.MoreHoriz)
}

@Composable
fun AppNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Transactions,
        BottomNavItem.Cards,
        BottomNavItem.Budget,
        BottomNavItem.More
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar && isLoggedIn) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
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
            startDestination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.AUTH,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(NavRoutes.AUTH) {
                val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AuthScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTransactions = {
                        navController.navigate(NavRoutes.TRANSACTIONS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToBudget = {
                        navController.navigate(NavRoutes.BUDGET) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(NavRoutes.TRANSACTIONS) {
                val txViewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                TransactionsScreen(viewModel = txViewModel)
            }
            composable(NavRoutes.CARDS) {
                val cardsViewModel: CreditCardsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                CreditCardsScreen(viewModel = cardsViewModel)
            }
            composable(NavRoutes.BUDGET) {
                val budgetViewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
                BudgetScreen(viewModel = budgetViewModel)
            }
            composable(NavRoutes.SAVINGS) {
                val savingsViewModel: SavingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                SavingsScreen(viewModel = savingsViewModel)
            }
            composable(NavRoutes.MORE) {
                MoreScreen(
                    onNavigateToSavings = { navController.navigate(NavRoutes.SAVINGS) },
                    onNavigateToDebts = { navController.navigate(NavRoutes.DEBTS) },
                    onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onLogout = {
                        appContainer.authRepository.logout()
                        isLoggedIn = false
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.DEBTS) {
                val debtsViewModel: DebtsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DebtsScreen(
                    viewModel = debtsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.HISTORY) {
                val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}