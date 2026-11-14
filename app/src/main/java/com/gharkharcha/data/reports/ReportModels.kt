package com.gharkharcha.data.reports

import com.gharkharcha.data.dao.AttendanceWithLabourer
import com.gharkharcha.data.dao.AttendanceWithWork
import com.gharkharcha.data.dao.PaymentWithLabourer
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import java.time.LocalDate
import java.time.LocalDateTime

enum class ReportType {
    HOUSE_SUMMARY,
    WORK_REPORT,
    LABOURER_STATEMENT,
}

enum class ReportFormat {
    PDF,
    CSV,
}

enum class DateRangePreset {
    ALL_TIME,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM,
}

data class DateFilter(
    val preset: DateRangePreset = DateRangePreset.ALL_TIME,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
) {
    fun matches(date: LocalDate): Boolean {
        return when (preset) {
            DateRangePreset.ALL_TIME -> true
            DateRangePreset.THIS_MONTH -> {
                val now = LocalDate.now()
                date.year == now.year && date.month == now.month
            }
            DateRangePreset.LAST_MONTH -> {
                val lastMonth = LocalDate.now().minusMonths(1)
                date.year == lastMonth.year && date.month == lastMonth.month
            }
            DateRangePreset.CUSTOM -> {
                val startOk = startDate == null || !date.isBefore(startDate)
                val endOk = endDate == null || !date.isAfter(endDate)
                startOk && endOk
            }
        }
    }

    fun displayText(): String {
        return when (preset) {
            DateRangePreset.ALL_TIME -> "All time"
            DateRangePreset.THIS_MONTH -> "This month (${LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }} ${LocalDate.now().year})"
            DateRangePreset.LAST_MONTH -> {
                val lm = LocalDate.now().minusMonths(1)
                "Last month (${lm.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${lm.year})"
            }
            DateRangePreset.CUSTOM -> {
                when {
                    startDate != null && endDate != null -> "$startDate to $endDate"
                    startDate != null -> "From $startDate"
                    endDate != null -> "Up to $endDate"
                    else -> "All time"
                }
            }
        }
    }
}

data class HouseWorkRow(
    val work: WorkEntity,
    val materialPaise: Long,
    val labourPaise: Long,
    val totalPaise: Long,
    val budgetPaise: Long,
    val percentUsed: Int,
)

data class MonthTotalRow(
    val yearMonth: String,
    val materialPaise: Long,
    val labourPaise: Long,
    val totalPaise: Long,
)

data class HouseSummaryReportData(
    val project: ProjectEntity,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val dateFilterText: String,
    val works: List<HouseWorkRow>,
    val totalMaterialPaise: Long,
    val totalLabourPaise: Long,
    val grandTotalPaise: Long,
    val totalBudgetPaise: Long,
    val balancePaise: Long,
    val unpaidShopPaise: Long,
    val monthlyTotals: List<MonthTotalRow>,
)

data class WorkReportData(
    val project: ProjectEntity,
    val work: WorkEntity,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val dateFilterText: String,
    val materials: List<MaterialEntity>,
    val attendance: List<AttendanceWithLabourer>,
    val payments: List<PaymentWithLabourer>,
    val totalMaterialPaise: Long,
    val totalLabourPaise: Long,
    val grandTotalPaise: Long,
    val budgetPaise: Long,
    val variancePaise: Long,
    val photoPaths: List<String> = emptyList(),
)

data class LabourerStatementData(
    val project: ProjectEntity?,
    val labourer: LabourerEntity,
    val photoPath: String?,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val dateFilterText: String,
    val attendance: List<AttendanceWithWork>,
    val payments: List<PaymentEntity>,
    val totalDays: Double,
    val totalMinutes: Long,
    val totalEarnedPaise: Long,
    val totalPaidPaise: Long,
    val netDuesPaise: Long,
)
