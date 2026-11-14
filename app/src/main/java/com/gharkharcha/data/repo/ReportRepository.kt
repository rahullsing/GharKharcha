package com.gharkharcha.data.repo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.LabourerDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.dao.ProjectDao
import com.gharkharcha.data.dao.WorkDao
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.data.reports.CsvReportGenerator
import com.gharkharcha.data.reports.DateFilter
import com.gharkharcha.data.reports.HouseSummaryReportData
import com.gharkharcha.data.reports.HouseWorkRow
import com.gharkharcha.data.reports.LabourerStatementData
import com.gharkharcha.data.reports.MonthTotalRow
import com.gharkharcha.data.reports.PdfReportGenerator
import com.gharkharcha.data.reports.ReportFormat
import com.gharkharcha.data.reports.ReportType
import com.gharkharcha.data.reports.WorkReportData
import com.gharkharcha.di.IoDispatcher
import com.gharkharcha.util.DateFormats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedReport(
    val file: File,
    val contentUri: Uri,
    val mimeType: String,
    val fileName: String,
)

@Singleton
class ReportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val workDao: WorkDao,
    private val materialDao: MaterialDao,
    private val attendanceDao: AttendanceDao,
    private val paymentDao: PaymentDao,
    private val labourerDao: LabourerDao,
    private val attachmentDao: AttachmentDao,
    private val attachmentFileStore: AttachmentFileStore,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    private val reportsDir: File get() = File(context.filesDir, "reports").apply { mkdirs() }

    suspend fun buildHouseSummaryData(projectId: Long, dateFilter: DateFilter): HouseSummaryReportData =
        withContext(io) {
            val project = projectDao.get(projectId)
                ?: projectDao.observeAll().first().firstOrNull()
                ?: ProjectEntity(id = 1, name = "House")
            val works = workDao.observeByProject(project.id).first()
            val allMaterials = materialDao.observeByProject(project.id).first()
                .filter { dateFilter.matches(it.purchaseDate) }

            val workRows = works.map { work ->
                val workMaterials = allMaterials.filter { it.workId == work.id }
                val matPaise = workMaterials.sumOf { it.totalPaise }
                val attList = attendanceDao.observeForWorkWithLabourer(work.id).first()
                    .filter { dateFilter.matches(it.attendance.date) }
                val labourPaise = attList.sumOf { it.attendance.totalPayPaise }
                val totalPaise = matPaise + labourPaise
                val percent = if (work.budgetPaise > 0) ((totalPaise * 100) / work.budgetPaise).toInt() else 0
                HouseWorkRow(
                    work = work,
                    materialPaise = matPaise,
                    labourPaise = labourPaise,
                    totalPaise = totalPaise,
                    budgetPaise = work.budgetPaise,
                    percentUsed = percent,
                )
            }

            val totalMatPaise = workRows.sumOf { it.materialPaise }
            val totalLabourPaise = workRows.sumOf { it.labourPaise }
            val grandTotalPaise = totalMatPaise + totalLabourPaise
            val totalBudgetPaise = works.sumOf { it.budgetPaise }
            val balancePaise = totalBudgetPaise - grandTotalPaise
            val unpaidShopPaise = allMaterials.filter { !it.isPaid }.sumOf { it.totalPaise }

            // Group monthly spend
            val monthlyMap = mutableMapOf<String, Pair<Long, Long>>() // YearMonth to (MaterialPaise, LabourPaise)
            allMaterials.forEach { m ->
                val key = m.purchaseDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))
                val current = monthlyMap[key] ?: Pair(0L, 0L)
                monthlyMap[key] = Pair(current.first + m.totalPaise, current.second)
            }
            works.forEach { work ->
                val attList = attendanceDao.observeForWorkWithLabourer(work.id).first()
                    .filter { dateFilter.matches(it.attendance.date) }
                attList.forEach { a ->
                    val key = a.attendance.date.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))
                    val current = monthlyMap[key] ?: Pair(0L, 0L)
                    monthlyMap[key] = Pair(current.first, current.second + a.attendance.totalPayPaise)
                }
            }
            val monthlyTotals = monthlyMap.map { (month, spend) ->
                MonthTotalRow(
                    yearMonth = month,
                    materialPaise = spend.first,
                    labourPaise = spend.second,
                    totalPaise = spend.first + spend.second,
                )
            }

            HouseSummaryReportData(
                project = project,
                dateFilterText = dateFilter.displayText(),
                works = workRows,
                totalMaterialPaise = totalMatPaise,
                totalLabourPaise = totalLabourPaise,
                grandTotalPaise = grandTotalPaise,
                totalBudgetPaise = totalBudgetPaise,
                balancePaise = balancePaise,
                unpaidShopPaise = unpaidShopPaise,
                monthlyTotals = monthlyTotals,
            )
        }

    suspend fun buildWorkReportData(
        workId: Long,
        dateFilter: DateFilter,
        includePhotos: Boolean,
    ): WorkReportData = withContext(io) {
        val work = workDao.get(workId) ?: throw IllegalArgumentException("Work not found")
        val project = projectDao.get(work.projectId) ?: ProjectEntity(id = work.projectId, name = "House")
        val materials = materialDao.observeByWork(workId).first()
            .filter { dateFilter.matches(it.purchaseDate) }
            .sortedBy { it.purchaseDate }
        val attendance = attendanceDao.observeForWorkWithLabourer(workId).first()
            .filter { dateFilter.matches(it.attendance.date) }
            .sortedBy { it.attendance.date }
        val payments = paymentDao.observeForWorkWithLabourer(workId).first()
            .filter { dateFilter.matches(it.payment.date) }
            .sortedBy { it.payment.date }

        val matPaise = materials.sumOf { it.totalPaise }
        val labourPaise = attendance.sumOf { it.attendance.totalPayPaise }
        val grandTotal = matPaise + labourPaise
        val variance = work.budgetPaise - grandTotal

        val photoPaths = if (includePhotos) {
            val attachments = attachmentDao.listPhotosForWorkReport(workId)
            attachments.map { attachmentFileStore.resolve(it.filePath).absolutePath }
                .filter { File(it).exists() }
        } else {
            emptyList()
        }

        WorkReportData(
            project = project,
            work = work,
            dateFilterText = dateFilter.displayText(),
            materials = materials,
            attendance = attendance,
            payments = payments,
            totalMaterialPaise = matPaise,
            totalLabourPaise = labourPaise,
            grandTotalPaise = grandTotal,
            budgetPaise = work.budgetPaise,
            variancePaise = variance,
            photoPaths = photoPaths,
        )
    }

    suspend fun buildLabourerStatementData(
        labourerId: Long,
        projectId: Long?,
        dateFilter: DateFilter,
    ): LabourerStatementData = withContext(io) {
        val labourer = labourerDao.get(labourerId) ?: throw IllegalArgumentException("Labourer not found")
        val project = if (projectId != null) projectDao.get(projectId) else null
        val attendance = attendanceDao.observeForLabourer(labourerId, projectId).first()
            .filter { dateFilter.matches(it.attendance.date) }
            .sortedBy { it.attendance.date }
        val payments = paymentDao.observeForLabourer(labourerId).first()
            .filter { (projectId == null || it.projectId == projectId) && dateFilter.matches(it.date) }
            .sortedBy { it.date }

        var days = 0.0
        var minutes = 0L
        attendance.forEach { a ->
            val att = a.attendance
            when (att.status) {
                AttendanceStatus.FULL_DAY, AttendanceStatus.OVERTIME -> days += 1.0
                AttendanceStatus.HALF_DAY -> days += 0.5
                AttendanceStatus.HOURS -> days += (att.minutesWorked / 480.0)
                AttendanceStatus.ABSENT -> {}
            }
            minutes += (att.minutesWorked + att.overtimeMinutes)
        }

        val earnedPaise = attendance.sumOf { it.attendance.totalPayPaise }
        val paidPaise = payments.sumOf { it.amountPaise }
        val duesPaise = earnedPaise - paidPaise

        val firstPhoto = attachmentDao.listFor(OwnerType.LABOURER, labourerId).firstOrNull()
        val photoPath = firstPhoto?.let { attachmentFileStore.resolve(it.filePath).absolutePath }

        LabourerStatementData(
            project = project,
            labourer = labourer,
            photoPath = photoPath,
            dateFilterText = dateFilter.displayText(),
            attendance = attendance,
            payments = payments,
            totalDays = days,
            totalMinutes = minutes,
            totalEarnedPaise = earnedPaise,
            totalPaidPaise = paidPaise,
            netDuesPaise = duesPaise,
        )
    }

    suspend fun exportReport(
        reportType: ReportType,
        format: ReportFormat,
        projectId: Long,
        workId: Long,
        labourerId: Long,
        dateFilter: DateFilter,
        includePhotos: Boolean = false,
    ): GeneratedReport = withContext(io) {
        val stamp = DateFormats.fileStamp()
        val extension = if (format == ReportFormat.PDF) "pdf" else "csv"
        val mimeType = if (format == ReportFormat.PDF) "application/pdf" else "text/csv"

        val fileName = when (reportType) {
            ReportType.HOUSE_SUMMARY -> "House_Summary_$stamp.$extension"
            ReportType.WORK_REPORT -> "Work_Report_${workId}_$stamp.$extension"
            ReportType.LABOURER_STATEMENT -> "Labourer_Statement_${labourerId}_$stamp.$extension"
        }

        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { out ->
            when (reportType) {
                ReportType.HOUSE_SUMMARY -> {
                    val data = buildHouseSummaryData(projectId, dateFilter)
                    if (format == ReportFormat.PDF) {
                        PdfReportGenerator.generateHouseSummary(data, out)
                    } else {
                        CsvReportGenerator.generateHouseSummary(data, out)
                    }
                }
                ReportType.WORK_REPORT -> {
                    val data = buildWorkReportData(workId, dateFilter, includePhotos)
                    if (format == ReportFormat.PDF) {
                        PdfReportGenerator.generateWorkReport(data, out)
                    } else {
                        CsvReportGenerator.generateWorkReport(data, out)
                    }
                }
                ReportType.LABOURER_STATEMENT -> {
                    val data = buildLabourerStatementData(labourerId, if (projectId > 0) projectId else null, dateFilter)
                    if (format == ReportFormat.PDF) {
                        PdfReportGenerator.generateLabourerStatement(data, out)
                    } else {
                        CsvReportGenerator.generateLabourerStatement(data, out)
                    }
                }
            }
        }

        // Save a copy to MediaStore in Documents/GharKharcha for public access
        saveToPublicDocuments(file, fileName, mimeType)

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )

        GeneratedReport(
            file = file,
            contentUri = contentUri,
            mimeType = mimeType,
            fileName = fileName,
        )
    }

    private fun saveToPublicDocuments(sourceFile: File, fileName: String, mimeType: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/GharKharcha")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val externalUri = MediaStore.Files.getContentUri("external")
            val itemUri = context.contentResolver.insert(externalUri, values)
            if (itemUri != null) {
                context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
            }
        } catch (_: Exception) {
            // Ignore MediaStore failures in restricted or test environments
        }
    }
}
