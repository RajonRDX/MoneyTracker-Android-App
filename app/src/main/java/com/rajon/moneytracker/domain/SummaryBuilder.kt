package com.rajon.moneytracker.domain

import com.rajon.moneytracker.data.model.*

object SummaryBuilder {
    fun buildMonthlySummary(
        yearMonth: String,
        monthSetup: MonthSetup?,
        transactions: List<Transaction>,
        budgets: List<CategoryBudget>,
        recurringList: List<RecurringPayment>,
        savingsDeposits: List<SavingsDeposit>,
        debtPayments: List<DebtPaymentRecord>,
        balanceBreakdown: BalanceBreakdown
    ): MonthlySummary {
        val totalIncome = balanceBreakdown.totalIncome
        val totalSpent = balanceBreakdown.totalSpent
        val totalBudgeted = budgets.sumOf { it.limitAmount + it.carryOverAdjustment }
        val savedAmount = (totalIncome - totalSpent).coerceAtLeast(0.0)

        val catBreakdown = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.categoryName.ifBlank { "Other" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val topCategories = catBreakdown.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val extraTotal = transactions
            .filter { it.type == "EXPENSE" && it.isExtra }
            .sumOf { it.amount }

        val debtTotal = debtPayments.sumOf { it.amount }
        val savingsDepTotal = savingsDeposits.filter { !it.isWithdrawal }.sumOf { it.amount }
        val savingsWdTotal = savingsDeposits.filter { it.isWithdrawal }.sumOf { it.amount }

        val recurringForMonth = recurringList.filter { it.active }
        val paidCount = recurringForMonth.count { it.paidMonths.contains(yearMonth) }
        val missedCount = recurringForMonth.count { !it.paidMonths.contains(yearMonth) && !it.skippedMonths.contains(yearMonth) }

        val savingsRate = if (totalIncome > 0.0) ((savedAmount / totalIncome) * 100) else 0.0

        return MonthlySummary(
            id = yearMonth,
            yearMonth = yearMonth,
            totalIncome = totalIncome,
            totalSpent = totalSpent,
            totalBudgeted = totalBudgeted,
            savedAmount = savedAmount,
            overallOverspend = (totalSpent - totalBudgeted).coerceAtLeast(0.0),
            extraSpendingTotal = extraTotal,
            categoryBreakdown = catBreakdown,
            categoryBudgetDiff = emptyMap(),
            recurringPaidCount = paidCount,
            recurringMissedCount = missedCount,
            debtPaymentsTotal = debtTotal,
            savingsDepositsTotal = savingsDepTotal,
            savingsWithdrawalsTotal = savingsWdTotal,
            closingBankBalance = balanceBreakdown.bankBalanceLeft,
            closingCashInHand = balanceBreakdown.cashInHand,
            topCategories = topCategories,
            savingsRatePercent = savingsRate
        )
    }

    fun generateInsights(currentSummary: MonthlySummary, previousSummary: MonthlySummary?): List<String> {
        val insights = mutableListOf<String>()
        if (previousSummary == null) {
            insights.add("Welcome to your first month summary! Keep tracking daily to unlock comparative insights.")
            return insights
        }

        val spendDiff = currentSummary.totalSpent - previousSummary.totalSpent
        if (spendDiff > 0) {
            val pct = if (previousSummary.totalSpent > 0) (spendDiff / previousSummary.totalSpent) * 100 else 0.0
            insights.add("You spent %.1f%% more this month compared to last month.".format(pct))
        } else if (spendDiff < 0) {
            insights.add("Great job! You spent less this month than last month.")
        }

        if (currentSummary.savingsRatePercent > previousSummary.savingsRatePercent) {
            insights.add("Your savings rate improved to %.1f%%!".format(currentSummary.savingsRatePercent))
        }

        return insights
    }
}
