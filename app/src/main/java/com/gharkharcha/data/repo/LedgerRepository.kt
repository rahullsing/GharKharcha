package com.gharkharcha.data.repo

import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.domain.calc.LedgerCalculator
import com.gharkharcha.domain.model.AttendanceTotals
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.domain.model.PaymentTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Days / Earned / Paid / Pending per labourer, live. */
@Singleton
class LedgerRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val paymentDao: PaymentDao,
    private val prefs: UserPreferencesRepository,
) {
    /**
     * Summaries keyed by labourer id. [projectId] / [workId] null = across everything.
     * Labourers with no attendance and no payments are absent from the map.
     */
    fun observeSummaries(projectId: Long? = null, workId: Long? = null): Flow<Map<Long, LabourSummary>> =
        combine(
            attendanceDao.observeTotalsByLabourer(projectId, workId),
            paymentDao.observeTotalsByLabourer(projectId, workId),
            prefs.hoursPerDay,
        ) { attendance, payments, hoursPerDay ->
            val att = attendance.associate { it.labourerId to it.toTotals() }
            val pay = payments.associate { it.labourerId to it.toTotals() }
            (att.keys + pay.keys).associateWith { id ->
                LedgerCalculator.summarize(
                    att[id] ?: AttendanceTotals(),
                    pay[id] ?: PaymentTotals(),
                    hoursPerDay,
                )
            }
        }
}
