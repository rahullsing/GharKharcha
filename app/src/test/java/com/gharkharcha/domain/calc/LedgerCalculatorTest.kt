package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.domain.model.BalanceState
import com.gharkharcha.domain.model.BudgetLevel
import com.gharkharcha.domain.model.SpendTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LedgerCalculatorTest {
    private val day = LocalDate.of(2026, 11, 2)

    private fun entry(
        status: AttendanceStatus,
        minutes: Int,
        base: Long,
        otMinutes: Int = 0,
        otPay: Long = 0,
        advance: Long = 0,
        offset: Long = 0,
    ) = AttendanceEntity(
        workId = 1, labourerId = 1, date = day.plusDays(offset), status = status,
        minutesWorked = minutes, basePayPaise = base, overtimeMinutes = otMinutes,
        overtimePayPaise = otPay, advancePaise = advance,
    )

    private fun payment(amount: Long, type: PaymentType = PaymentType.WAGE) =
        PaymentEntity(labourerId = 1, date = day, amountPaise = amount, type = type)

    @Test fun `summary of a typical week`() {
        val entries = listOf(
            entry(AttendanceStatus.FULL_DAY, 480, 700_00, offset = 0),
            entry(AttendanceStatus.FULL_DAY, 480, 700_00, offset = 1, advance = 200_00),
            entry(AttendanceStatus.HALF_DAY, 240, 350_00, offset = 2),
            entry(AttendanceStatus.HOURS, 240, 350_00, offset = 3),
            entry(AttendanceStatus.ABSENT, 0, 0, offset = 4),
            entry(AttendanceStatus.OVERTIME, 480, 700_00, otMinutes = 120, otPay = 200_00, offset = 5),
        )
        val payments = listOf(payment(1_500_00), payment(500_00, PaymentType.ADVANCE))

        val s = LedgerCalculator.summarize(entries, payments, hoursPerDay = 8)

        // 3 full (incl. overtime) + 1 half + 4h = 3 + 0.5 + 0.5 = 4 days
        assertEquals(400L, s.dayHundredths)
        // 480+480+240+240+480 + 120 OT = 2040 min
        assertEquals(2_040L, s.totalMinutes)
        assertEquals(3_000_00L, s.earnedPaise)
        assertEquals(1_500_00L, s.wagesPaidPaise)
        assertEquals(700_00L, s.advancesPaise) // 200 on attendance + 500 advance payment
        assertEquals(2_200_00L, s.paidPaise)
        assertEquals(800_00L, s.pendingPaise)
        assertEquals(BalanceState.DUE, s.balanceState)
    }

    @Test fun `overpaid labourer shows advance balance`() {
        val s = LedgerCalculator.summarize(
            listOf(entry(AttendanceStatus.FULL_DAY, 480, 700_00)),
            listOf(payment(1_000_00, PaymentType.ADVANCE)),
            8,
        )
        assertEquals(-300_00L, s.pendingPaise)
        assertEquals(BalanceState.ADVANCE, s.balanceState)
    }

    @Test fun `settled and empty`() {
        val s = LedgerCalculator.summarize(
            listOf(entry(AttendanceStatus.FULL_DAY, 480, 700_00)), listOf(payment(700_00)), 8,
        )
        assertEquals(0L, s.pendingPaise)
        assertEquals(BalanceState.SETTLED, s.balanceState)

        val empty = LedgerCalculator.summarize(emptyList(), emptyList(), 8)
        assertEquals(0L, empty.dayHundredths)
        assertEquals(BalanceState.SETTLED, empty.balanceState)
    }

    @Test fun `spend totals and budget levels`() {
        val t = SpendTotals(materialPaise = 60_000_00, labourPaise = 20_000_00, budgetPaise = 1_00_000_00)
        assertEquals(80_000_00L, t.grandTotalPaise)
        assertEquals(20_000_00L, t.variancePaise)
        assertEquals(8_000L, t.usedBasisPoints)
        assertEquals(0.8f, t.progress, 0.0001f)
        assertEquals(BudgetLevel.WARNING, t.budgetLevel)

        assertEquals(BudgetLevel.OK, t.copy(materialPaise = 50_000_00).budgetLevel)
        assertEquals(BudgetLevel.REACHED, t.copy(materialPaise = 80_000_00).budgetLevel)
        val over = t.copy(materialPaise = 1_00_000_00)
        assertEquals(BudgetLevel.OVER, over.budgetLevel)
        assertEquals(-20_000_00L, over.variancePaise)
        assertEquals(1f, over.progress, 0.0001f)

        val noBudget = t.copy(budgetPaise = 0)
        assertNull(noBudget.usedBasisPoints)
        assertEquals(BudgetLevel.NO_BUDGET, noBudget.budgetLevel)
        assertEquals(0f, noBudget.progress, 0f)
    }

    @Test fun `spend totals add`() {
        val sum = SpendTotals(1, 2, 3) + SpendTotals(10, 20, 30)
        assertEquals(SpendTotals(11, 22, 33), sum)
    }
}
