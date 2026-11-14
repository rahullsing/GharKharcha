package com.gharkharcha.domain.model

/** Budget vs actual for a Work or Project. */
data class SpendTotals(
    val materialPaise: Long,
    val labourPaise: Long,
    val budgetPaise: Long,
) {
    val grandTotalPaise: Long get() = materialPaise + labourPaise

    /** Budget minus spend; negative when over budget. */
    val variancePaise: Long get() = budgetPaise - grandTotalPaise

    /** Spend as basis points of budget (10000 = 100%); null when no budget is set. */
    val usedBasisPoints: Long?
        get() = if (budgetPaise <= 0) null else grandTotalPaise * 10_000 / budgetPaise

    /** 0f..1f for progress bars (clamped). */
    val progress: Float
        get() = usedBasisPoints?.let { (it.coerceIn(0, 10_000)) / 10_000f } ?: 0f

    val budgetLevel: BudgetLevel
        get() {
            val bp = usedBasisPoints ?: return BudgetLevel.NO_BUDGET
            return when {
                bp > 10_000 -> BudgetLevel.OVER
                bp >= 10_000 -> BudgetLevel.REACHED
                bp >= 8_000 -> BudgetLevel.WARNING
                else -> BudgetLevel.OK
            }
        }

    operator fun plus(other: SpendTotals) = SpendTotals(
        materialPaise = materialPaise + other.materialPaise,
        labourPaise = labourPaise + other.labourPaise,
        budgetPaise = budgetPaise + other.budgetPaise,
    )

    companion object {
        val ZERO = SpendTotals(0, 0, 0)
    }
}

/** Thresholds used for budget alerts: 80% warning, 100% reached. */
enum class BudgetLevel { NO_BUDGET, OK, WARNING, REACHED, OVER }
