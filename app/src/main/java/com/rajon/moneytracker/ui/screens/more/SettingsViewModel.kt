package com.rajon.moneytracker.ui.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajon.moneytracker.data.local.LocalPrefs
import com.rajon.moneytracker.data.model.UserSettings
import com.rajon.moneytracker.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val financeRepository: FinanceRepository,
    private val localPrefs: LocalPrefs
) : ViewModel() {

    val settings: StateFlow<UserSettings> = financeRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            financeRepository.updateSettings(newSettings)
            localPrefs.isLocked = newSettings.biometricEnabled
        }
    }
}
