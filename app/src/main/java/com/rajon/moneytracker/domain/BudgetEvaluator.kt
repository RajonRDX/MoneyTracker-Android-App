package com.rajon.moneytracker.domain

import com.rajon.moneytracker.data.model.CategoryBudget
import com.rajon.moneytracker.data.model.Transaction

data class BudgetStatus(
    val categoryId: String,
    val categoryName: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val percentage: Float,
    val isOverBudget: Boolean,
    val isWarning: Boolean // 70% to 99%
)

object BudgetEvaluator {
    fun evaluateBudgets(
        budgets: List<CategoryBudget>,
        transactions: List<Transaction>,
        overallLimit: Double,
        extraLimit: Double
    ): List<BudgetStatus> {
        val spentMap = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        return budgets.map { b ->
            val spent = spentMap[b.categoryId] ?: 0.0
            val effectiveLimit = b.limitAmount + b.carryOverAdjustment
            val pct = if (effectiveLimit > 0.0) ((spent / effectiveLimit) * 100).toFloat() else 0f
            BudgetStatus(
                categoryId = b.categoryId,
                categoryName = b.categoryName,
                limitAmount = effectiveLimit,
                spentAmount = spent,
                percentage = pct,
                isOverBudget = spent >= effectiveLimit && effectiveLimit > 0.0,
                isWarning = pct >= 70f && pct < 100f
            )
        }
    }
}
