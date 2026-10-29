package com.rajon.moneytracker.ui.screens.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToDebts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("More & Tools") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToDebts) {
                    ListItem(
                        headlineContent = { Text("Debts & Loans Tracker") },
                        leadingContent = { Icon(Icons.Default.Payments, contentDescription = null) }
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToHistory) {
                    ListItem(
                        headlineContent = { Text("History & Monthly Reports") },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToSettings) {
                    ListItem(
                        headlineContent = { Text("Settings") },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onLogout) {
                    ListItem(
                        headlineContent = { Text("Logout") },
                        leadingContent = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
