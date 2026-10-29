package com.rajon.moneytracker.domain

import com.rajon.moneytracker.data.model.DebtPaymentRecord
import com.rajon.moneytracker.data.model.MonthSetup
import com.rajon.moneytracker.data.model.PaymentSource
import com.rajon.moneytracker.data.model.SavingsDeposit
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType

data class BalanceBreakdown(
    val openingBankBalance: Double,
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

    val totalSpent: Double
)

object BalanceCalculator {
    /**
     * Calculates exact bank balance left and cash in hand for a given month.
     * Logic strictly adheres to project contract:
     * Bank balance = opening balance + income + loans taken + repayments received
     *                - withdrawals - bank/card/wallet expenses - debt payments from bank
     *                - loans given from bank - savings transfers taken from bank.
     * Cash in hand = withdrawals - cash expenses - cash debt payments.
     */
    fun calculate(
        monthSetup: MonthSetup?,
        transactions: List<Transaction>,
        savingsDeposits: List<SavingsDeposit>,
        debtPayments: List<DebtPaymentRecord>
    ): BalanceBreakdown {
        val opening = monthSetup?.openingBankBalance ?: 0.0
        val salary = monthSetup?.salary ?: 0.0
        val extraInc = monthSetup?.extraIncome ?: 0.0
        val totalIncome = salary + extraInc

        // Filter transactions for this month or general
        var loansTaken = 0.0
        var repaymentsReceived = 0.0
        var withdrawals = 0.0
        var bankExpenses = 0.0
        var cardExpenses = 0.0
        var walletExpenses = 0.0
        var cashExpenses = 0.0
        var loansGivenFromBank = 0.0

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME.name -> {
                    // Extra income or loan received via transaction
                    if (tx.note.contains("Loan", ignoreCase = true)) {
                        loansTaken += tx.amount
                    }
                }
                TransactionType.WITHDRAWAL.name -> {
                    withdrawals += tx.amount
                }
                TransactionType.EXPENSE.name -> {
                    val isBank = tx.paymentSource == PaymentSource.BANK.name
                    val isCard = tx.paymentSource == PaymentSource.CARD.name
                    val isWallet = tx.paymentSource == PaymentSource.BKASH.name || tx.paymentSource == PaymentSource.NAGAD.name
                    val isCash = tx.paymentSource == PaymentSource.CASH.name

                    if (isBank) bankExpenses += tx.amount
                    else if (isCard) cardExpenses += tx.amount
                    else if (isWallet) walletExpenses += tx.amount
                    else if (isCash) cashExpenses += tx.amount

                    if (tx.note.contains("Lend", ignoreCase = true) && isBank) {
                        loansGivenFromBank += tx.amount
                    }
                }
            }
        }

        // Debt payments from bank vs cash
        var debtPaymentsFromBank = 0.0
        var cashDebtPayments = 0.0
        for (dp in debtPayments) {
            if (dp.paymentSource == PaymentSource.BANK.name || dp.paymentSource == PaymentSource.CARD.name || dp.paymentSource == PaymentSource.BKASH.name || dp.paymentSource == PaymentSource.NAGAD.name) {
                debtPaymentsFromBank += dp.amount
            } else {
                cashDebtPayments += dp.amount
            }
        }

        // Savings from bank
        var savingsFromBank = 0.0
        for (dep in savingsDeposits) {
            if (dep.takeFromBank && !dep.isWithdrawal) {
                savingsFromBank += dep.amount
            } else if (dep.takeFromBank && dep.isWithdrawal) {
                // Withdrawal from savings back to bank
                withdrawals -= dep.amount // effectively adds back to bank or reduces transfer
            }
        }

        val totalSpent = bankExpenses + cardExpenses + walletExpenses + cashExpenses
        val totalBankOutflow = withdrawals + bankExpenses + cardExpenses + walletExpenses + debtPaymentsFromBank + loansGivenFromBank + savingsFromBank
        val totalBankInflow = opening + totalIncome + loansTaken + repaymentsReceived

        val bankBalanceLeft = totalBankInflow - totalBankOutflow
        val cashInHand = withdrawals - cashExpenses - cashDebtPayments

        return BalanceBreakdown(
            openingBankBalance = opening,
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
            totalSpent = totalSpent
        )
    }
}
