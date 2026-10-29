package com.rajon.moneytracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rajon.moneytracker.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    override val currentUserIdFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, pass: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }
}

class FirestoreFinanceRepository : FinanceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val userDoc
        get() = firestore.collection("users").document(userId)

    override fun observeSettings(): Flow<UserSettings> = callbackFlow {
        val listener = userDoc.collection("settings").document("profile")
            .addSnapshotListener { snapshot, _ ->
                val settings = snapshot?.toObject(UserSettings::class.java) ?: UserSettings()
                trySend(settings)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        userDoc.collection("settings").document("profile").set(settings).await()
    }

    override fun observeMonthSetup(yearMonth: String): Flow<MonthSetup?> = callbackFlow {
        val listener = userDoc.collection("month_setups").document(yearMonth)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                } else {
                    val setup = snapshot.toObject(MonthSetup::class.java)?.copy(
                        yearMonth = yearMonth
                    )
                    trySend(setup)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllMonthSetups(): Flow<List<MonthSetup>> = callbackFlow {
        val listener = userDoc.collection("month_setups")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MonthSetup::class.java)?.copy(yearMonth = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveMonthSetup(setup: MonthSetup) {
        userDoc.collection("month_setups").document(setup.yearMonth).set(setup).await()
    }

    override fun observeCategories(): Flow<List<ExpenseCategory>> = callbackFlow {
        val listener = userDoc.collection("categories")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(ExpenseCategory::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun ensureDefaultCategories() {
        val ref = userDoc.collection("categories")
        val snapshot = ref.get().await()
        if (snapshot.isEmpty) {
            val defaults = listOf(
                ExpenseCategory(id = "food", name = "Food & Dining", colorHex = 0xFFEF5350L, iconName = "restaurant", isDefault = true),
                ExpenseCategory(id = "transport", name = "Transportation", colorHex = 0xFF42A5F5L, iconName = "directions_car", isDefault = true),
                ExpenseCategory(id = "utilities", name = "Utilities & Bills", colorHex = 0xFFFFCA28L, iconName = "bolt", isDefault = true),
                ExpenseCategory(id = "shopping", name = "Shopping", colorHex = 0xFFAB47BCL, iconName = "shopping_bag", isDefault = true)
            )
            val batch = firestore.batch()
            for (cat in defaults) {
                batch.set(ref.document(cat.id), cat)
            }
            batch.commit().await()
        }
    }

    override suspend fun saveCategory(category: ExpenseCategory) {
        val id = if (category.id.isBlank()) userDoc.collection("categories").document().id else category.id
        val toSave = category.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("categories").document(id).set(toSave).await()
    }

    override suspend fun deleteCategory(categoryId: String) {
        userDoc.collection("categories").document(categoryId).delete().await()
    }

    override fun observeTransactions(yearMonth: String): Flow<List<Transaction>> = callbackFlow {
        val listener = userDoc.collection("transactions")
            .whereEqualTo("yearMonth", yearMonth)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val listener = userDoc.collection("transactions")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveTransaction(tx: Transaction) {
        val id = if (tx.id.isBlank()) userDoc.collection("transactions").document().id else tx.id
        val toSave = tx.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("transactions").document(id).set(toSave).await()
    }

    override suspend fun deleteTransaction(txId: String) {
        userDoc.collection("transactions").document(txId).delete().await()
    }

    override fun observeRecurring(): Flow<List<RecurringPayment>> = callbackFlow {
        val listener = userDoc.collection("recurring")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(RecurringPayment::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveRecurring(item: RecurringPayment) {
        val id = if (item.id.isBlank()) userDoc.collection("recurring").document().id else item.id
        val toSave = item.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("recurring").document(id).set(toSave).await()
    }

    override suspend fun deleteRecurring(id: String) {
        userDoc.collection("recurring").document(id).delete().await()
    }

    override suspend fun markRecurringPaid(item: RecurringPayment, yearMonth: String, dateMillis: Long) {
        val updatedPaid = item.paidMonths + yearMonth
        val toSave = item.copy(paidMonths = updatedPaid, updatedAt = System.currentTimeMillis())
        userDoc.collection("recurring").document(item.id).set(toSave).await()
    }

    override suspend fun markRecurringSkipped(item: RecurringPayment, yearMonth: String) {
        val updatedSkipped = item.skippedMonths + yearMonth
        val toSave = item.copy(skippedMonths = updatedSkipped, updatedAt = System.currentTimeMillis())
        userDoc.collection("recurring").document(item.id).set(toSave).await()
    }

    override fun observeBudgets(yearMonth: String): Flow<List<CategoryBudget>> = callbackFlow {
        val listener = userDoc.collection("budgets")
            .whereEqualTo("yearMonth", yearMonth)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(CategoryBudget::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveBudget(budget: CategoryBudget) {
        val id = if (budget.id.isBlank()) "${budget.yearMonth}_${budget.categoryId}" else budget.id
        val toSave = budget.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("budgets").document(id).set(toSave).await()
    }

    override fun observeGoals(): Flow<List<SavingsGoal>> = callbackFlow {
        val listener = userDoc.collection("goals")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsGoal::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeGoalDeposits(goalId: String): Flow<List<SavingsDeposit>> = callbackFlow {
        val listener = userDoc.collection("deposits")
            .whereEqualTo("goalId", goalId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeMonthDeposits(yearMonth: String): Flow<List<SavingsDeposit>> = callbackFlow {
        val listener = userDoc.collection("deposits")
            .whereEqualTo("yearMonth", yearMonth)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllDeposits(): Flow<List<SavingsDeposit>> = callbackFlow {
        val listener = userDoc.collection("deposits")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveGoal(goal: SavingsGoal) {
        val id = if (goal.id.isBlank()) userDoc.collection("goals").document().id else goal.id
        val toSave = goal.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("goals").document(id).set(toSave).await()
    }

    override suspend fun deleteGoal(goalId: String) {
        userDoc.collection("goals").document(goalId).delete().await()
    }

    override suspend fun addGoalDeposit(goal: SavingsGoal, deposit: SavingsDeposit) {
        val depositId = if (deposit.id.isBlank()) userDoc.collection("deposits").document().id else deposit.id
        val savedDeposit = deposit.copy(id = depositId, goalId = goal.id, updatedAt = System.currentTimeMillis())
        
        val newCurrent = if (deposit.isWithdrawal) goal.currentAmount - deposit.amount else goal.currentAmount + deposit.amount
        val updatedGoal = goal.copy(currentAmount = newCurrent, updatedAt = System.currentTimeMillis())

        val batch = firestore.batch()
        batch.set(userDoc.collection("deposits").document(depositId), savedDeposit)
        batch.set(userDoc.collection("goals").document(goal.id), updatedGoal)
        batch.commit().await()
    }

    override fun observeDebts(): Flow<List<DebtItem>> = callbackFlow {
        val listener = userDoc.collection("debts")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(DebtItem::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeDebtPayments(debtId: String): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val listener = userDoc.collection("debt_payments")
            .whereEqualTo("debtId", debtId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeMonthDebtPayments(yearMonth: String): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val listener = userDoc.collection("debt_payments")
            .whereEqualTo("yearMonth", yearMonth)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllDebtPayments(): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val listener = userDoc.collection("debt_payments")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveDebt(debt: DebtItem) {
        val id = if (debt.id.isBlank()) userDoc.collection("debts").document().id else debt.id
        val toSave = debt.copy(id = id, updatedAt = System.currentTimeMillis())
        userDoc.collection("debts").document(id).set(toSave).await()
    }

    override suspend fun deleteDebt(debtId: String) {
        userDoc.collection("debts").document(debtId).delete().await()
    }

    override suspend fun addDebtPayment(debt: DebtItem, payment: DebtPaymentRecord, createExpenseTx: Boolean) {
        val paymentId = if (payment.id.isBlank()) userDoc.collection("debt_payments").document().id else payment.id
        val savedPayment = payment.copy(id = paymentId, debtId = debt.id, updatedAt = System.currentTimeMillis())
        
        val newPaid = debt.paidAmount + payment.amount
        val newStatus = if (newPaid >= debt.totalAmount) DebtStatus.CLOSED.name else DebtStatus.ACTIVE.name
        val updatedDebt = debt.copy(paidAmount = newPaid, status = newStatus, updatedAt = System.currentTimeMillis())

        val batch = firestore.batch()
        batch.set(userDoc.collection("debt_payments").document(paymentId), savedPayment)
        batch.set(userDoc.collection("debts").document(debt.id), updatedDebt)
        batch.commit().await()
    }

    override fun observeMonthlySummaries(): Flow<List<MonthlySummary>> = callbackFlow {
        val listener = userDoc.collection("monthly_summaries")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(MonthlySummary::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeMonthlySummary(yearMonth: String): Flow<MonthlySummary?> = callbackFlow {
        val listener = userDoc.collection("monthly_summaries").document(yearMonth)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(MonthlySummary::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveMonthlySummary(summary: MonthlySummary) {
        userDoc.collection("monthly_summaries").document(summary.yearMonth).set(summary).await()
    }

    override suspend fun getSnapshotForWorker(): WorkerDataSnapshot {
        // Implementation for background worker snapshots if needed
        return WorkerDataSnapshot(UserSettings(), emptyList(), emptyList())
    }

    override suspend fun exportFullBackupData(): FullBackupPayload {
        return FullBackupPayload(
            version = 1,
            settings = null,
            monthSetups = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
            recurring = emptyList(),
            budgets = emptyList(),
            goals = emptyList(),
            goalDeposits = emptyList(),
            debts = emptyList(),
            debtPayments = emptyList(),
            summaries = emptyList()
        )
    }

    override suspend fun restoreFullBackupData(payload: FullBackupPayload) {
        // Restore logic if needed
    }
}