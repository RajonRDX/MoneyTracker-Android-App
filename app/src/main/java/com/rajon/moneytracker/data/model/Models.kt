package com.rajon.moneytracker.data.model

import java.io.Serializable

enum class TransactionType {
    EXPENSE, INCOME, WITHDRAWAL, CARD_LOAN, CARD_PAYMENT
}

enum class PaymentSource(val displayName: String) {
    BANK("Bank"),
    CASH("Cash"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    CARD("Card")
}

enum class DebtType(val displayName: String) {
    I_OWE("I Owe"),
    OWED_TO_ME("Owed to Me")
}

enum class DebtStatus(val displayName: String) {
    ACTIVE("Active"),
    CLOSED("Closed")
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserSettings(
    val id: String = "profile",
    val currencySymbol: String = "৳",
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = false,
    val biometricEnabled: Boolean = false,
    val overallMonthlyLimit: Double = 0.0,
    val extraSpendingLimit: Double = 0.0,
    val reminderHour: Int = 9,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class MonthSetup(
    val yearMonth: String = "",
    val openingBalance: Double = 0.0,
    val openingBankBalance: Double = 0.0,
    val salary: Double = 0.0,
    val extraIncome: Double = 0.0,
    val incomeAmount: Double = 0.0,
    val expectedSavings: Double = 0.0,
    val overallBudgetLimit: Double = 0.0,
    val extraSpendingLimit: Double = 0.0
) : Serializable

data class ExpenseCategory(
    val id: String = "",
    val name: String = "",
    val colorHex: Long = 0xFF00897BL,
    val iconName: String = "category",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class Transaction(
    val id: String = "",
    val yearMonth: String = "",
    val type: String = "EXPENSE",
    val amount: Double = 0.0,
    val dateMillis: Long = 0L,
    val categoryId: String = "",
    val categoryName: String = "",
    val paymentSource: String = "CASH",
    val note: String = "",
    val isExtra: Boolean = false,
    val isOneOffBill: Boolean = false,
    val linkedRecurringId: String? = null,
    val linkedDebtId: String? = null,
    val linkedCardId: String? = null,
    val linkedCardName: String = "",
    val isCardBillPayment: Boolean = false,
    val emiMonths: Int = 1,
    val cardLoanPrincipal: Double = 0.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class CreditCard(
    val id: String = "",
    val cardName: String = "",
    val bankName: String = "",
    val lastFourDigits: String = "",
    val creditLimit: Double = 0.0,
    val previousDebt: Double = 0.0,
    val statementDay: Int = 15,
    val paymentDueDay: Int = 5,
    val minimumDuePercent: Double = 5.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class RecurringPayment(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val dueDay: Int = 1,
    val categoryId: String = "",
    val categoryName: String = "",
    val paymentSource: String = "BANK",
    val remindDaysBefore: Int = 3,
    val active: Boolean = true,
    val paidMonths: List<String> = emptyList(),
    val skippedMonths: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class CategoryBudget(
    val id: String = "",
    val yearMonth: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val limitAmount: Double = 0.0,
    val carryOverAdjustment: Double = 0.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long? = null,
    val iconName: String = "savings",
    val colorHex: Long = 0xFF26A69AL,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class SavingsDeposit(
    val id: String = "",
    val goalId: String = "",
    val yearMonth: String = "",
    val amount: Double = 0.0,
    val isWithdrawal: Boolean = false,
    val takeFromBank: Boolean = true,
    val dateMillis: Long = 0L,
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class DebtItem(
    val id: String = "",
    val type: String = "I_OWE",
    val personName: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val monthlyInstallment: Double = 0.0,
    val dueDay: Int = 1,
    val specificDueDateMillis: Long? = null,
    val remindDaysBefore: Int = 3,
    val interestRate: Double = 0.0,
    val note: String = "",
    val status: String = "ACTIVE",
    val affectBankOnCreate: Boolean = false,
    val createdInYearMonth: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class DebtPaymentRecord(
    val id: String = "",
    val debtId: String = "",
    val debtType: String = "I_OWE",
    val yearMonth: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = 0L,
    val paymentSource: String = "BANK",
    val note: String = "",
    val linkedTransactionId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class MonthlySummary(
    val id: String = "",
    val yearMonth: String = "",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalBudgeted: Double = 0.0,
    val savedAmount: Double = 0.0,
    val overallOverspend: Double = 0.0,
    val extraSpendingTotal: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val categoryBudgetDiff: Map<String, Double> = emptyMap(),
    val recurringPaidCount: Int = 0,
    val recurringMissedCount: Int = 0,
    val debtPaymentsTotal: Double = 0.0,
    val savingsDepositsTotal: Double = 0.0,
    val savingsWithdrawalsTotal: Double = 0.0,
    val closingBankBalance: Double = 0.0,
    val closingCashInHand: Double = 0.0,
    val topCategories: List<String> = emptyList(),
    val savingsRatePercent: Double = 0.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Serializable

data class WorkerDataSnapshot(
    val settings: UserSettings,
    val recurringList: List<RecurringPayment>,
    val debtsList: List<DebtItem>
)

data class FullBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: UserSettings?,
    val monthSetups: List<MonthSetup>,
    val categories: List<ExpenseCategory>,
    val transactions: List<Transaction>,
    val recurring: List<RecurringPayment>,
    val budgets: List<CategoryBudget>,
    val goals: List<SavingsGoal>,
    val goalDeposits: List<SavingsDeposit>,
    val debts: List<DebtItem>,
    val debtPayments: List<DebtPaymentRecord>,
    val summaries: List<MonthlySummary>,
    val creditCards: List<CreditCard> = emptyList()
) : Serializable