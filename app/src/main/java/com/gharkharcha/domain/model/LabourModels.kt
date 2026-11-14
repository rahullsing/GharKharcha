package com.gharkharcha.domain.model

/** Raw attendance sums; days are derived later because they depend on hours-per-day. */
data class AttendanceTotals(
    val fullDays: Int = 0,
    val halfDays: Int = 0,
    /** Minutes from entries with status HOURS (converted to fractional days). */
    val hourlyMinutes: Long = 0,
    /** All minutes worked including overtime. */
    val totalMinutes: Long = 0,
    val earnedPaise: Long = 0,
    /** Advances recorded on attendance entries. */
    val advancePaise: Long = 0,
)

data class PaymentTotals(
    val wagePaise: Long = 0,
    val advancePaise: Long = 0,
)

/**
 * Ledger summary for a labourer. Days are fixed-point hundredths (4.5 days = 450).
 * [pendingPaise] > 0 means money is due to the labourer; < 0 means they were overpaid / hold an advance.
 */
data class LabourSummary(
    val dayHundredths: Long,
    val totalMinutes: Long,
    val earnedPaise: Long,
    val wagesPaidPaise: Long,
    val advancesPaise: Long,
) {
    val paidPaise: Long get() = wagesPaidPaise + advancesPaise
    val pendingPaise: Long get() = earnedPaise - paidPaise
    val balanceState: BalanceState
        get() = when {
            pendingPaise > 0 -> BalanceState.DUE
            pendingPaise < 0 -> BalanceState.ADVANCE
            else -> BalanceState.SETTLED
        }

    companion object {
        val EMPTY = LabourSummary(0, 0, 0, 0, 0)
    }
}

enum class BalanceState { DUE, SETTLED, ADVANCE }
