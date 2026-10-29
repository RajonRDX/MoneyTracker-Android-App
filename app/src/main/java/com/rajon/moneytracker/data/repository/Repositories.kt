package com.rajon.moneytracker.data.repository

import com.rajon.moneytracker.data.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserIdFlow: Flow<String?>
    val currentUserEmail: String?
    val isLoggedIn: Boolean

    suspend fun login(email: String, pass: String): Result<Unit>
    suspend fun register(email: String, pass: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

interface FinanceRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)

    fun observeMonthSetup(yearMonth: String): Flow<MonthSetup?>
    fun observeAllMonthSetups(): Flow<List<MonthSetup>>
    suspend fun saveMonthSetup(setup: MonthSetup)

    fun observeCategories(): Flow<List<ExpenseCategory>>
    suspend fun ensureDefaultCategories()
    suspend fun saveCategory(category: ExpenseCategory)
    suspend fun deleteCategory(categoryId: String)

    fun observeTransactions(yearMonth: String): Flow<List<Transaction>>
    fun observeAllTransactions(): Flow<List<Transaction>>
    suspend fun saveTransaction(tx: Transaction)
    suspend fun deleteTransaction(txId: String)

    fun observeRecurring(): Flow<List<RecurringPayment>>
    suspend fun saveRecurring(item: RecurringPayment)
    suspend fun deleteRecurring(id: String)
    suspend fun markRecurringPaid(item: RecurringPayment, yearMonth: String, dateMillis: Long)
    suspend fun markRecurringSkipped(item: RecurringPayment, yearMonth: String)

    fun observeBudgets(yearMonth: String): Flow<List<CategoryBudget>>
    suspend fun saveBudget(budget: CategoryBudget)

    fun observeGoals(): Flow<List<SavingsGoal>>
    fun observeGoalDeposits(goalId: String): Flow<List<SavingsDeposit>>
    fun observeMonthDeposits(yearMonth: String): Flow<List<SavingsDeposit>>
    fun observeAllDeposits(): Flow<List<SavingsDeposit>>
    suspend fun saveGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goalId: String)
    suspend fun addGoalDeposit(goal: SavingsGoal, deposit: SavingsDeposit)

    fun observeDebts(): Flow<List<DebtItem>>
    fun observeDebtPayments(debtId: String): Flow<List<DebtPaymentRecord>>
    fun observeMonthDebtPayments(yearMonth: String): Flow<List<DebtPaymentRecord>>
    fun observeAllDebtPayments(): Flow<List<DebtPaymentRecord>>
    suspend fun saveDebt(debt: DebtItem)
    suspend fun deleteDebt(debtId: String)
    suspend fun addDebtPayment(debt: DebtItem, payment: DebtPaymentRecord, createExpenseTx: Boolean)

    fun observeMonthlySummaries(): Flow<List<MonthlySummary>>
    fun observeMonthlySummary(yearMonth: String): Flow<MonthlySummary?>
    suspend fun saveMonthlySummary(summary: MonthlySummary)

    suspend fun getSnapshotForWorker(): WorkerDataSnapshot
    suspend fun exportFullBackupData(): FullBackupPayload
    suspend fun restoreFullBackupData(payload: FullBackupPayload)
}
