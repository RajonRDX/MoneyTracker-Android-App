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

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

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

class FirestoreFinanceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FinanceRepository {

    private val uid: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    private val userRef
        get() = firestore.collection("users").document(uid)

    override fun observeSettings(): Flow<UserSettings> = callbackFlow {
        val ref = userRef.collection("settings").document("profile")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val settings = snapshot?.toObject(UserSettings::class.java) ?: UserSettings()
            trySend(settings)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updateSettings(settings: UserSettings) {
        val now = System.currentTimeMillis()
        val updated = settings.copy(updatedAt = now, createdAt = if (settings.createdAt == 0L) now else settings.createdAt)
        userRef.collection("settings").document("profile").set(updated).await()
    }

    override fun observeMonthSetup(yearMonth: String): Flow<MonthSetup?> = callbackFlow {
        val ref = userRef.collection("months").document(yearMonth)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val setup = snapshot?.toObject(MonthSetup::class.java)
            trySend(setup)
        }
        awaitClose { listener.remove() }
    }

    override fun observeAllMonthSetups(): Flow<List<MonthSetup>> = callbackFlow {
        val ref = userRef.collection("months")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(MonthSetup::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveMonthSetup(setup: MonthSetup) {
        val now = System.currentTimeMillis()
        val docId = setup.yearMonth.ifBlank { "default" }
        val finalSetup = setup.copy(
            id = docId,
            createdAt = if (setup.createdAt == 0L) now else setup.createdAt,
            updatedAt = now
        )
        userRef.collection("months").document(docId).set(finalSetup).await()
    }

    override fun observeCategories(): Flow<List<ExpenseCategory>> = callbackFlow {
        val ref = userRef.collection("categories").orderBy("name", Query.Direction.ASCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(ExpenseCategory::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun ensureDefaultCategories() {
        val col = userRef.collection("categories")
        val snapshot = col.get().await()
        if (!snapshot.isEmpty) return

        val defaults = listOf(
            Triple("Food", 0xFFE53935L, "restaurant"),
            Triple("Transport", 0xFF1E88E5L, "directions_car"),
            Triple("Family", 0xFF8E24AAL, "family_restroom"),
            Triple("Wife", 0xFFD81B60L, "favorite"),
            Triple("Rent", 0xFF43A047L, "home"),
            Triple("Bills", 0xFFFB8C00L, "receipt_long"),
            Triple("Internet", 0xFF00ACC1L, "wifi"),
            Triple("Mobile", 0xFF3949ABL, "phone_android"),
            Triple("Health", 0xFFE91E63L, "local_hospital"),
            Triple("Shopping", 0xFF7CB342L, "shopping_bag"),
            Triple("Education", 0xFF5E35B1L, "school"),
            Triple("Debt Payment", 0xFF6D4C41L, "payments"),
            Triple("Other", 0xFF546E7AL, "category")
        )

        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        for ((name, color, icon) in defaults) {
            val docRef = col.document()
            val cat = ExpenseCategory(
                id = docRef.id,
                name = name,
                colorHex = color,
                iconName = icon,
                isDefault = true,
                createdAt = now,
                updatedAt = now
            )
            batch.set(docRef, cat)
        }
        batch.commit().await()
    }

    override suspend fun saveCategory(category: ExpenseCategory) {
        val now = System.currentTimeMillis()
        val id = category.id.ifBlank { userRef.collection("categories").document().id }
        val cat = category.copy(
            id = id,
            createdAt = if (category.createdAt == 0L) now else category.createdAt,
            updatedAt = now
        )
        userRef.collection("categories").document(id).set(cat).await()
    }

    override suspend fun deleteCategory(categoryId: String) {
        userRef.collection("categories").document(categoryId).delete().await()
    }

    override fun observeTransactions(yearMonth: String): Flow<List<Transaction>> = callbackFlow {
        val ref = userRef.collection("transactions")
            .whereEqualTo("yearMonth", yearMonth)
            .orderBy("dateMillis", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val ref = userRef.collection("transactions").orderBy("dateMillis", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveTransaction(tx: Transaction) {
        val now = System.currentTimeMillis()
        val id = tx.id.ifBlank { userRef.collection("transactions").document().id }
        val finalTx = tx.copy(
            id = id,
            createdAt = if (tx.createdAt == 0L) now else tx.createdAt,
            updatedAt = now
        )
        userRef.collection("transactions").document(id).set(finalTx).await()
    }

    override suspend fun deleteTransaction(txId: String) {
        userRef.collection("transactions").document(txId).delete().await()
    }

    override fun observeRecurring(): Flow<List<RecurringPayment>> = callbackFlow {
        val ref = userRef.collection("recurring").orderBy("dueDay", Query.Direction.ASCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(RecurringPayment::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveRecurring(item: RecurringPayment) {
        val now = System.currentTimeMillis()
        val id = item.id.ifBlank { userRef.collection("recurring").document().id }
        val finalItem = item.copy(
            id = id,
            createdAt = if (item.createdAt == 0L) now else item.createdAt,
            updatedAt = now
        )
        userRef.collection("recurring").document(id).set(finalItem).await()
    }

    override suspend fun deleteRecurring(id: String) {
        userRef.collection("recurring").document(id).delete().await()
    }

    override suspend fun markRecurringPaid(item: RecurringPayment, yearMonth: String, dateMillis: Long) {
        val updatedMonths = (item.paidMonths + yearMonth).distinct()
        val updatedRecurring = item.copy(paidMonths = updatedMonths, updatedAt = System.currentTimeMillis())
        saveRecurring(updatedRecurring)

        // Automatically create expense transaction
        val tx = Transaction(
            yearMonth = yearMonth,
            type = TransactionType.EXPENSE.name,
            amount = item.amount,
            dateMillis = dateMillis,
            categoryId = item.categoryId,
            categoryName = item.categoryName,
            paymentSource = item.paymentSource,
            note = "Recurring bill: ${item.name}",
            linkedRecurringId = item.id
        )
        saveTransaction(tx)
    }

    override suspend fun markRecurringSkipped(item: RecurringPayment, yearMonth: String) {
        val updatedSkipped = (item.skippedMonths + yearMonth).distinct()
        val updatedRecurring = item.copy(skippedMonths = updatedSkipped, updatedAt = System.currentTimeMillis())
        saveRecurring(updatedRecurring)
    }

    override fun observeBudgets(yearMonth: String): Flow<List<CategoryBudget>> = callbackFlow {
        val ref = userRef.collection("budgets").whereEqualTo("yearMonth", yearMonth)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(CategoryBudget::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveBudget(budget: CategoryBudget) {
        val now = System.currentTimeMillis()
        val id = budget.id.ifBlank { "${budget.yearMonth}_${budget.categoryId}" }
        val finalBudget = budget.copy(
            id = id,
            createdAt = if (budget.createdAt == 0L) now else budget.createdAt,
            updatedAt = now
        )
        userRef.collection("budgets").document(id).set(finalBudget).await()
    }

    override fun observeGoals(): Flow<List<SavingsGoal>> = callbackFlow {
        val ref = userRef.collection("goals").orderBy("name", Query.Direction.ASCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsGoal::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeGoalDeposits(goalId: String): Flow<List<SavingsDeposit>> = callbackFlow {
        val ref = userRef.collection("goals").document(goalId).collection("deposits")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeMonthDeposits(yearMonth: String): Flow<List<SavingsDeposit>> = callbackFlow {
        // Collects across all goal deposits matching yearMonth
        val goalsSnapshot = userRef.collection("goals").get().await()
        val goalIds = goalsSnapshot.documents.map { it.id }
        if (goalIds.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        // For simplicity, observe all goals and query deposits
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val depositsMap = mutableMapOf<String, List<SavingsDeposit>>()

        goalIds.forEach { gId ->
            val l = userRef.collection("goals").document(gId).collection("deposits")
                .whereEqualTo("yearMonth", yearMonth)
                .addSnapshotListener { snap, _ ->
                    val deps = snap?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
                    depositsMap[gId] = deps
                    trySend(depositsMap.values.flatten().sortedByDescending { it.dateMillis })
                }
            if (l != null) listeners.add(l)
        }

        awaitClose { listeners.forEach { it.remove() } }
    }

    override fun observeAllDeposits(): Flow<List<SavingsDeposit>> = callbackFlow {
        val goalsSnapshot = userRef.collection("goals").get().await()
        val goalIds = goalsSnapshot.documents.map { it.id }
        if (goalIds.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val map = mutableMapOf<String, List<SavingsDeposit>>()

        goalIds.forEach { gId ->
            val l = userRef.collection("goals").document(gId).collection("deposits")
                .addSnapshotListener { snap, _ ->
                    map[gId] = snap?.documents?.mapNotNull { it.toObject(SavingsDeposit::class.java) } ?: emptyList()
                    trySend(map.values.flatten().sortedByDescending { it.dateMillis })
                }
            if (l != null) listeners.add(l)
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    override suspend fun saveGoal(goal: SavingsGoal) {
        val now = System.currentTimeMillis()
        val id = goal.id.ifBlank { userRef.collection("goals").document().id }
        val finalGoal = goal.copy(
            id = id,
            createdAt = if (goal.createdAt == 0L) now else goal.createdAt,
            updatedAt = now
        )
        userRef.collection("goals").document(id).set(finalGoal).await()
    }

    override suspend fun deleteGoal(goalId: String) {
        userRef.collection("goals").document(goalId).delete().await()
    }

    override suspend fun addGoalDeposit(goal: SavingsGoal, deposit: SavingsDeposit) {
        val now = System.currentTimeMillis()
        val depId = deposit.id.ifBlank { userRef.collection("goals").document(goal.id).collection("deposits").document().id }
        val finalDep = deposit.copy(
            id = depId,
            goalId = goal.id,
            createdAt = if (deposit.createdAt == 0L) now else deposit.createdAt,
            updatedAt = now
        )
        userRef.collection("goals").document(goal.id).collection("deposits").document(depId).set(finalDep).await()

        // Update goal currentAmount
        val delta = if (deposit.isWithdrawal) -deposit.amount else deposit.amount
        val newAmount = (goal.currentAmount + delta).coerceAtLeast(0.0)
        val updatedGoal = goal.copy(currentAmount = newAmount, updatedAt = now)
        userRef.collection("goals").document(goal.id).set(updatedGoal).await()

        // If takeFromBank and it's a deposit, optionally record as a withdrawal transaction
        if (deposit.takeFromBank && !deposit.isWithdrawal) {
            val tx = Transaction(
                yearMonth = deposit.yearMonth,
                type = TransactionType.WITHDRAWAL.name,
                amount = deposit.amount,
                dateMillis = deposit.dateMillis,
                paymentSource = PaymentSource.BANK.name,
                note = "Savings deposit: ${goal.name}"
            )
            saveTransaction(tx)
        }
    }

    override fun observeDebts(): Flow<List<DebtItem>> = callbackFlow {
        val ref = userRef.collection("debts").orderBy("personName", Query.Direction.ASCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(DebtItem::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeDebtPayments(debtId: String): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val ref = userRef.collection("debts").document(debtId).collection("payments")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeMonthDebtPayments(yearMonth: String): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val debtsSnapshot = userRef.collection("debts").get().await()
        val debtIds = debtsSnapshot.documents.map { it.id }
        if (debtIds.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val map = mutableMapOf<String, List<DebtPaymentRecord>>()

        debtIds.forEach { dId ->
            val l = userRef.collection("debts").document(dId).collection("payments")
                .whereEqualTo("yearMonth", yearMonth)
                .addSnapshotListener { snap, _ ->
                    map[dId] = snap?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
                    trySend(map.values.flatten().sortedByDescending { it.dateMillis })
                }
            if (l != null) listeners.add(l)
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    override fun observeAllDebtPayments(): Flow<List<DebtPaymentRecord>> = callbackFlow {
        val debtsSnapshot = userRef.collection("debts").get().await()
        val debtIds = debtsSnapshot.documents.map { it.id }
        if (debtIds.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val map = mutableMapOf<String, List<DebtPaymentRecord>>()

        debtIds.forEach { dId ->
            val l = userRef.collection("debts").document(dId).collection("payments")
                .addSnapshotListener { snap, _ ->
                    map[dId] = snap?.documents?.mapNotNull { it.toObject(DebtPaymentRecord::class.java) } ?: emptyList()
                    trySend(map.values.flatten().sortedByDescending { it.dateMillis })
                }
            if (l != null) listeners.add(l)
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    override suspend fun saveDebt(debt: DebtItem) {
        val now = System.currentTimeMillis()
        val id = debt.id.ifBlank { userRef.collection("debts").document().id }
        val finalDebt = debt.copy(
            id = id,
            createdAt = if (debt.createdAt == 0L) now else debt.createdAt,
            updatedAt = now
        )
        userRef.collection("debts").document(id).set(finalDebt).await()
    }

    override suspend fun deleteDebt(debtId: String) {
        userRef.collection("debts").document(debtId).delete().await()
    }

    override suspend fun addDebtPayment(debt: DebtItem, payment: DebtPaymentRecord, createExpenseTx: Boolean) {
        val now = System.currentTimeMillis()
        val payId = payment.id.ifBlank { userRef.collection("debts").document(debt.id).collection("payments").document().id }
        val finalPay = payment.copy(
            id = payId,
            debtId = debt.id,
            createdAt = if (payment.createdAt == 0L) now else payment.createdAt,
            updatedAt = now
        )
        userRef.collection("debts").document(debt.id).collection("payments").document(payId).set(finalPay).await()

        val newPaid = (debt.paidAmount + payment.amount).coerceAtMost(debt.totalAmount)
        val newStatus = if (newPaid >= debt.totalAmount) DebtStatus.CLOSED.name else DebtStatus.ACTIVE.name
        val updatedDebt = debt.copy(paidAmount = newPaid, status = newStatus, updatedAt = now)
        userRef.collection("debts").document(debt.id).set(updatedDebt).await()

        if (createExpenseTx && debt.type == DebtType.I_OWE.name) {
            val tx = Transaction(
                yearMonth = payment.yearMonth,
                type = TransactionType.EXPENSE.name,
                amount = payment.amount,
                dateMillis = payment.dateMillis,
                categoryId = "",
                categoryName = "Debt Payment",
                paymentSource = payment.paymentSource,
                note = "Debt payment to ${debt.personName}",
                linkedDebtId = debt.id
            )
            saveTransaction(tx)
        }
    }

    override fun observeMonthlySummaries(): Flow<List<MonthlySummary>> = callbackFlow {
        val ref = userRef.collection("monthlySummaries").orderBy("yearMonth", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(MonthlySummary::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    override fun observeMonthlySummary(yearMonth: String): Flow<MonthlySummary?> = callbackFlow {
        val ref = userRef.collection("monthlySummaries").document(yearMonth)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val summary = snapshot?.toObject(MonthlySummary::class.java)
            trySend(summary)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun saveMonthlySummary(summary: MonthlySummary) {
        val now = System.currentTimeMillis()
        val id = summary.yearMonth
        val finalSummary = summary.copy(
            id = id,
            createdAt = if (summary.createdAt == 0L) now else summary.createdAt,
            updatedAt = now
        )
        userRef.collection("monthlySummaries").document(id).set(finalSummary).await()
    }

    override suspend fun getSnapshotForWorker(): WorkerDataSnapshot {
        val settingsSnap = userRef.collection("settings").document("profile").get().await()
        val settings = settingsSnap.toObject(UserSettings::class.java) ?: UserSettings()

        val recSnap = userRef.collection("recurring").get().await()
        val recurringList = recSnap.documents.mapNotNull { it.toObject(RecurringPayment::class.java) }

        val debtSnap = userRef.collection("debts").get().await()
        val debtsList = debtSnap.documents.mapNotNull { it.toObject(DebtItem::class.java) }

        return WorkerDataSnapshot(settings, recurringList, debtsList)
    }

    override suspend fun exportFullBackupData(): FullBackupPayload {
        val settings = userRef.collection("settings").document("profile").get().await().toObject(UserSettings::class.java)
        val months = userRef.collection("months").get().await().documents.mapNotNull { it.toObject(MonthSetup::class.java) }
        val cats = userRef.collection("categories").get().await().documents.mapNotNull { it.toObject(ExpenseCategory::class.java) }
        val txs = userRef.collection("transactions").get().await().documents.mapNotNull { it.toObject(Transaction::class.java) }
        val recs = userRef.collection("recurring").get().await().documents.mapNotNull { it.toObject(RecurringPayment::class.java) }
        val buds = userRef.collection("budgets").get().await().documents.mapNotNull { it.toObject(CategoryBudget::class.java) }
        val goals = userRef.collection("goals").get().await().documents.mapNotNull { it.toObject(SavingsGoal::class.java) }
        
        val goalDeps = mutableListOf<SavingsDeposit>()
        for (g in goals) {
            val deps = userRef.collection("goals").document(g.id).collection("deposits").get().await()
                .documents.mapNotNull { it.toObject(SavingsDeposit::class.java) }
            goalDeps.addAll(deps)
        }

        val debts = userRef.collection("debts").get().await().documents.mapNotNull { it.toObject(DebtItem::class.java) }
        val debtPays = mutableListOf<DebtPaymentRecord>()
        for (d in debts) {
            val pays = userRef.collection("debts").document(d.id).collection("payments").get().await()
                .documents.mapNotNull { it.toObject(DebtPaymentRecord::class.java) }
            debtPays.addAll(pays)
        }

        val sums = userRef.collection("monthlySummaries").get().await().documents.mapNotNull { it.toObject(MonthlySummary::class.java) }

        return FullBackupPayload(
            settings = settings,
            monthSetups = months,
            categories = cats,
            transactions = txs,
            recurring = recs,
            budgets = buds,
            goals = goals,
            goalDeposits = goalDeps,
            debts = debts,
            debtPayments = debtPays,
            summaries = sums
        )
    }

    override suspend fun restoreFullBackupData(payload: FullBackupPayload) {
        if (payload.settings != null) {
            updateSettings(payload.settings)
        }
        for (m in payload.monthSetups) saveMonthSetup(m)
        for (c in payload.categories) saveCategory(c)
        for (t in payload.transactions) saveTransaction(t)
        for (r in payload.recurring) saveRecurring(r)
        for (b in payload.budgets) saveBudget(b)
        for (g in payload.goals) {
            saveGoal(g)
            val deps = payload.goalDeposits.filter { it.goalId == g.id }
            for (dep in deps) {
                userRef.collection("goals").document(g.id).collection("deposits").document(dep.id).set(dep).await()
            }
        }
        for (d in payload.debts) {
            saveDebt(d)
            val pays = payload.debtPayments.filter { it.debtId == d.id }
            for (p in pays) {
                userRef.collection("debts").document(d.id).collection("payments").document(p.id).set(p).await()
            }
        }
        for (s in payload.summaries) saveMonthlySummary(s)
    }
}
