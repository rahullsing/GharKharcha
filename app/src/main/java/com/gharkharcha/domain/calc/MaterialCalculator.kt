package com.gharkharcha.domain.calc

/** Quantity is fixed-point thousandths (2.5 = 2500). */
object MaterialCalculator {
    const val QTY_SCALE = 1000L

    /** qty × rate, rounded half-up to the paisa. */
    fun total(quantityMilli: Long, ratePaise: Long): Long =
        PayCalculator.divRound(quantityMilli * ratePaise, QTY_SCALE)
}
