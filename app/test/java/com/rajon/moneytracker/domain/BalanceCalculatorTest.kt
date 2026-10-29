package com.rajon.moneytracker.domain

import com.rajon.moneytracker.data.model.MonthSetup
import com.rajon.moneytracker.data.model.PaymentSource
import com.rajon.moneytracker.data.model.Transaction
import com.rajon.moneytracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun testBalanceCalculationLogic() {
        val setup = MonthSetup(
            yearMonth = "2026-05",
            salary = 50000.0,
            extraIncome = 5000.0,
            openingBankBalance = 20000.0
        )

        val txs = listOf(
            Transaction(
                amount = 10000.0,
                type = TransactionType.WITHDRAWAL.name,
                paymentSource = PaymentSource.BANK.name
            ),
            Transaction(
                amount = 3000.0,
                type = TransactionType.EXPENSE.name,
                paymentSource = PaymentSource.CASH.name
            ),
            Transaction(
                amount = 5000.0,
                type = TransactionType.EXPENSE.name,
                paymentSource = PaymentSource.BANK.name
            )
        )

        val breakdown = BalanceCalculator.calculate(setup, txs, emptyList(), emptyList())

        // Inflow: 20000 (opening) + 55000 (income) = 75000
        // Outflow from bank: 10000 (withdrawal) + 5000 (bank expense) = 15000
        // Bank balance left: 75000 - 15000 = 60000
        assertEquals(60000.0, breakdown.bankBalanceLeft, 0.001)

        // Cash in hand: 10000 (withdrawal) - 3000 (cash expense) = 7000
        assertEquals(7000.0, breakdown.cashInHand, 0.001)
        assertEquals(8000.0, breakdown.totalSpent, 0.001)
    }
}
