package com.rajon.moneytracker.domain

import com.rajon.moneytracker.data.model.DebtPaymentRecord
import com.rajon.moneytracker.data.model.MonthSetup
import com.rajon.moneytracker.data.model.PaymentSource
import com.rajon.moneytracker.data.model.SavingsDeposit
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType

data class BalanceBreakdown(
    val openingBankBalance: Double,
    val openingCashBalance: Double,
    val setupSalary: Double,
    val setupExtraIncome: Double,
    val transactionIncomeBank: Double,
    val transactionIncomeCash: Double,
    val totalIncome: Double,
    val totalLoansTaken: Double,
    val totalRepaymentsReceived: Double,
    val totalWithdrawals: Double,
    val totalBankExpenses: Double,
    val totalCardExpenses: Double,
    val totalWalletExpenses: Double,
    val totalDebtPaymentsFromBank: Double,
    val totalLoansGivenFromBank: Double,
    val totalSavingsFromBank: Double,
    val bankBalanceLeft: Double,
    val totalCashExpenses: Double,
    val totalCashDebtPayments: Double,
    val cashInHand: Double,
    val totalSpent: Double,
    val isCarriedOverFromPrevMonth: Boolean = false
)

object BalanceCalculator {
    fun calculate(
        monthSetup: MonthSetup?,
        transactions: List<Transaction>,
        savingsDeposits: List<SavingsDeposit>,
        debtPayments: List<DebtPaymentRecord>,
        carriedOverBankBalance: Double = 0.0,
        carriedOverCashBalance: Double = 0.0,
        carriedOverSalary: Double = 0.0,
        isCarriedOver: Boolean = false
    ): BalanceBreakdown {
        val openingBank = if (monthSetup != null) monthSetup.openingBankBalance else carriedOverBankBalance
        val openingCash = if (monthSetup != null) monthSetup.openingBalance else carriedOverCashBalance
        val salary = if (monthSetup != null && (monthSetup.salary > 0.0 || transactions.any { it.type == TransactionType.INCOME.name })) {
            monthSetup.salary
        } else if (monthSetup != null) {
            monthSetup.salary
        } else {
            carriedOverSalary
        }
        val extraInc = monthSetup?.extraIncome ?: 0.0

        var loansTaken = 0.0
        var repaymentsReceived = 0.0
        var txIncomeBank = 0.0
        var txIncomeCash = 0.0
        var withdrawals = 0.0
        var bankExpenses = 0.0
        var cardExpenses = 0.0
        var walletExpenses = 0.0
        var cashExpenses = 0.0
        var loansGivenFromBank = 0.0

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME.name -> {
                    if (tx.note.contains("Loan", ignoreCase = true)) {
                        loansTaken += tx.amount
                    } else if (tx.note.contains("Repay", ignoreCase = true)) {
                        repaymentsReceived += tx.amount
                    } else {
                        if (tx.paymentSource == PaymentSource.CASH.name) {
                            txIncomeCash += tx.amount
                        } else {
                            txIncomeBank += tx.amount
                        }
                    }
                }
                TransactionType.WITHDRAWAL.name -> {
                    withdrawals += tx.amount
                }
                TransactionType.EXPENSE.name -> {
                    when (tx.paymentSource) {
                        PaymentSource.BANK.name -> bankExpenses += tx.amount
                        PaymentSource.CARD.name -> cardExpenses += tx.amount
                        PaymentSource.BKASH.name, PaymentSource.NAGAD.name -> walletExpenses += tx.amount
                        PaymentSource.CASH.name -> cashExpenses += tx.amount
                        else -> cashExpenses += tx.amount
                    }

                    if (tx.note.contains("Lend", ignoreCase = true) && tx.paymentSource == PaymentSource.BANK.name) {
                        loansGivenFromBank += tx.amount
                    }
                }
            }
        }

        var debtPaymentsFromBank = 0.0
        var cashDebtPayments = 0.0
        for (dp in debtPayments) {
            if (dp.paymentSource == PaymentSource.CASH.name) {
                cashDebtPayments += dp.amount
            } else {
                debtPaymentsFromBank += dp.amount
            }
        }

        var savingsFromBank = 0.0
        for (dep in savingsDeposits) {
            if (dep.takeFromBank && !dep.isWithdrawal) {
                savingsFromBank += dep.amount
            } else if (dep.takeFromBank && dep.isWithdrawal) {
                savingsFromBank -= dep.amount
            }
        }

        val totalIncome = salary + extraInc + txIncomeBank + txIncomeCash
        val totalSpent = bankExpenses + cardExpenses + walletExpenses + cashExpenses
        val totalBankOutflow = withdrawals + bankExpenses + cardExpenses + walletExpenses + debtPaymentsFromBank + loansGivenFromBank + savingsFromBank
        val totalBankInflow = openingBank + salary + extraInc + txIncomeBank + loansTaken + repaymentsReceived

        val bankBalanceLeft = totalBankInflow - totalBankOutflow
        val cashInHand = openingCash + withdrawals + txIncomeCash - cashExpenses - cashDebtPayments

        return BalanceBreakdown(
            openingBankBalance = openingBank,
            openingCashBalance = openingCash,
            setupSalary = salary,
            setupExtraIncome = extraInc,
            transactionIncomeBank = txIncomeBank,
            transactionIncomeCash = txIncomeCash,
            totalIncome = totalIncome,
            totalLoansTaken = loansTaken,
            totalRepaymentsReceived = repaymentsReceived,
            totalWithdrawals = withdrawals,
            totalBankExpenses = bankExpenses,
            totalCardExpenses = cardExpenses,
            totalWalletExpenses = walletExpenses,
            totalDebtPaymentsFromBank = debtPaymentsFromBank,
            totalLoansGivenFromBank = loansGivenFromBank,
            totalSavingsFromBank = savingsFromBank,
            bankBalanceLeft = bankBalanceLeft,
            totalCashExpenses = cashExpenses,
            totalCashDebtPayments = cashDebtPayments,
            cashInHand = cashInHand,
            totalSpent = totalSpent,
            isCarriedOverFromPrevMonth = isCarriedOver
        )
    }
}