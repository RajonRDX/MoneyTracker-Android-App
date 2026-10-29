package com.rajon.moneytracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajon.moneytracker.data.di.AppViewModelProvider
import com.rajon.moneytracker.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & Monthly Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (summaries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(text = "No monthly closing reports available yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(summaries, key = { it.yearMonth }) { summary ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = Formatters.formatMonthTitle(summary.yearMonth), style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Income: ${Formatters.formatCurrency(summary.totalIncome)}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Spent: ${Formatters.formatCurrency(summary.totalSpent)}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Saved: ${Formatters.formatCurrency(summary.savedAmount)}", style = MaterialTheme.typography.bodyMedium, color = com.rajon.moneytracker.ui.theme.GreenSuccess)
                            }
                        }
                    }
                }
            }
        }
    }
}
