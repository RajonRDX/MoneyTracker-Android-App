package com.rajon.moneytracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.model.MonthlySummary
import com.rajon.moneytracker.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*

class HistoryViewModel(
    financeRepository: FinanceRepository
) : ViewModel() {

    val summaries: StateFlow<List<MonthlySummary>> = financeRepository.observeMonthlySummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
