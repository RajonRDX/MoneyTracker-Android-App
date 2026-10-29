package com.rajon.moneytracker.data.di

import android.content.Context
import com.rajon.moneytracker.data.local.LocalPrefs
import com.rajon.moneytracker.data.repository.AuthRepository
import com.rajon.moneytracker.data.repository.FirebaseAuthRepository
import com.rajon.moneytracker.data.repository.FinanceRepository
import com.rajon.moneytracker.data.repository.FirestoreFinanceRepository

class AppContainer(val context: Context) {
    val localPrefs: LocalPrefs by lazy { LocalPrefs(context) }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val financeRepository: FinanceRepository by lazy { FirestoreFinanceRepository() }
}
