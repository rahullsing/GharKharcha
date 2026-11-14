package com.gharkharcha.data.reports

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import java.io.File
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT // 523f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 46f
    private const val FOOTER_Y = PAGE_HEIGHT - 24f

    private val COLOR_PRIMARY = Color.rgb(0, 106, 96)
    private val COLOR_PRIMARY_CONTAINER = Color.rgb(225, 245, 240)
    private val COLOR_TEXT_DARK = Color.rgb(30, 41, 59)
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)
    private val COLOR_BORDER = Color.rgb(226, 232, 240)
    private val COLOR_ZEBRA = Color.rgb(248, 250, 252)
    private val COLOR_WHITE = Color.WHITE
    private val COLOR_RED = Color.rgb(186, 26, 26)
    private val COLOR_GREEN = Color.rgb(20, 108, 67)

    private class PdfPageContext(
        val document: PdfDocument,
        val reportTitle: String,
        val subtitle: String,
        val generatedAtText: String,
    ) {
        val pages = mutableListOf<PdfDocument.Page>()
        var currentPageIndex = 0
        var currentCanvas: Canvas? = null
        var y: Float = MARGIN_TOP

        fun startNewPage(): Canvas {
            if (pages.isNotEmpty()) {
                document.finishPage(pages.last())
            }
            currentPageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageIndex).create()
            val page = document.startPage(pageInfo)
            pages.add(page)
            val canvas = page.canvas
            currentCanvas = canvas
            y = MARGIN_TOP

            // Draw top header bar
            drawPageHeader(canvas)
            return canvas
        }

        private fun drawPageHeader(canvas: Canvas) {
            val bgPaint = Paint().apply {
                color = COLOR_PRIMARY
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT + CONTENT_WIDTH, MARGIN_TOP + 32f, bgPaint)

            val textPaint = Paint().apply {
                color = COLOR_WHITE
                textSize = 13f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("GHARKHARCHA", MARGIN_LEFT + 10f, MARGIN_TOP + 21f, textPaint)

            val titlePaint = Paint().apply {
                color = COLOR_WHITE
                textSize = 10f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val titleWidth = titlePaint.measureText(reportTitle)
            canvas.drawText(reportTitle, MARGIN_LEFT + CONTENT_WIDTH - titleWidth - 10f, MARGIN_TOP + 20f, titlePaint)

            y = MARGIN_TOP + 42f
        }

        fun ensureSpace(neededHeight: Float, onNewPageHeader: (() -> Unit)? = null): Canvas {
            if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                val canvas = startNewPage()
                onNewPageHeader?.invoke()
                return canvas
            }
            return currentCanvas ?: startNewPage()
        }

        fun finishAll() {
            if (pages.isNotEmpty()) {
                // Finish last open page
                document.finishPage(pages.last())
            }
        }
    }

    private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.length > 1 && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    fun generateHouseSummary(data: HouseSummaryReportData, output: OutputStream) {
        val document = PdfDocument()
        val generatedAtText = data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US))
        val context = PdfPageContext(
            document = document,
            reportTitle = "House Summary Report",
            subtitle = data.project.name,
            generatedAtText = generatedAtText,
        )

        val canvas = context.startNewPage()

        // Document Meta header
        drawReportMetaHeader(
            context = context,
            title = data.project.name,
            address = data.project.address,
            dateRange = data.dateFilterText,
            generatedAt = generatedAtText,
        )

        // Summary KPI Cards (2x3 grid)
        drawKpiCards(
            context = context,
            cards = listOf(
                Pair("Total Budget", Money.format(data.totalBudgetPaise)),
                Pair("Grand Total Spent", Money.format(data.grandTotalPaise)),
                Pair("Remaining Balance", Money.format(data.balancePaise)),
                Pair("Materials Spent", Money.format(data.totalMaterialPaise)),
                Pair("Labour Spent", Money.format(data.totalLabourPaise)),
                Pair("Unpaid to Shops", Money.format(data.unpaidShopPaise)),
            ),
        )

        // Section: Works Table
        drawSectionHeader(context, "BREAKDOWN BY WORK")
        val workColumns = listOf(
            TableColumn("S.No", 32f, Align.CENTER),
            TableColumn("Work Name", 160f, Align.LEFT),
            TableColumn("Status", 65f, Align.LEFT),
            TableColumn("Budget", 76f, Align.RIGHT),
            TableColumn("Materials", 70f, Align.RIGHT),
            TableColumn("Labour", 60f, Align.RIGHT),
            TableColumn("Total", 60f, Align.RIGHT),
        )

        drawTable(
            context = context,
            columns = workColumns,
            rows = data.works.mapIndexed { index, row ->
                listOf(
                    (index + 1).toString(),
                    row.work.name,
                    row.work.status.name,
                    Money.format(row.budgetPaise),
                    Money.format(row.materialPaise),
                    Money.format(row.labourPaise),
                    Money.format(row.totalPaise),
                )
            },
            totalRow = listOf(
                "Total",
                "",
                "",
                Money.format(data.totalBudgetPaise),
                Money.format(data.totalMaterialPaise),
                Money.format(data.totalLabourPaise),
                Money.format(data.grandTotalPaise),
            ),
        )

        // Section: Monthly Breakdown
        if (data.monthlyTotals.isNotEmpty()) {
            context.ensureSpace(60f)
            drawSectionHeader(context, "MONTHLY SPEND")
            val monthColumns = listOf(
                TableColumn("Month", 123f, Align.LEFT),
                TableColumn("Materials Spend", 130f, Align.RIGHT),
                TableColumn("Labour Spend", 130f, Align.RIGHT),
                TableColumn("Total Spend", 140f, Align.RIGHT),
            )
            drawTable(
                context = context,
                columns = monthColumns,
                rows = data.monthlyTotals.map {
                    listOf(
                        it.yearMonth,
                        Money.format(it.materialPaise),
                        Money.format(it.labourPaise),
                        Money.format(it.totalPaise),
                    )
                },
                totalRow = listOf(
                    "Total",
                    Money.format(data.totalMaterialPaise),
                    Money.format(data.totalLabourPaise),
                    Money.format(data.grandTotalPaise),
                ),
            )
        }

        context.finishAll()
        document.writeTo(output)
        document.close()
    }

    fun generateWorkReport(data: WorkReportData, output: OutputStream) {
        val document = PdfDocument()
        val generatedAtText = data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US))
        val context = PdfPageContext(
            document = document,
            reportTitle = "Work Detailed Report",
            subtitle = "${data.project.name} · ${data.work.name}",
            generatedAtText = generatedAtText,
        )

        context.startNewPage()

        // Meta Header
        drawReportMetaHeader(
            context = context,
            title = "${data.work.name} (${data.work.status.name})",
            address = "House: ${data.project.name}",
            dateRange = data.dateFilterText,
            generatedAt = generatedAtText,
        )

        // KPI Cards
        val varianceLabel = if (data.variancePaise >= 0) "Under Budget" else "Over Budget"
        drawKpiCards(
            context = context,
            cards = listOf(
                Pair("Work Budget", Money.format(data.budgetPaise)),
                Pair("Grand Total Spent", Money.format(data.grandTotalPaise)),
                Pair("Variance ($varianceLabel)", Money.format(kotlin.math.abs(data.variancePaise))),
                Pair("Materials Cost", Money.format(data.totalMaterialPaise)),
                Pair("Labour Cost", Money.format(data.totalLabourPaise)),
                Pair("Payments Made", Money.format(data.payments.sumOf { it.payment.amountPaise })),
            ),
        )

        // 1. Materials Table
        drawSectionHeader(context, "1. MATERIALS PURCHASED (${data.materials.size})")
        val matColumns = listOf(
            TableColumn("S.No", 28f, Align.CENTER),
            TableColumn("Date", 56f, Align.LEFT),
            TableColumn("Item & Brand", 140f, Align.LEFT),
            TableColumn("Qty", 55f, Align.LEFT),
            TableColumn("Rate", 60f, Align.RIGHT),
            TableColumn("Amount", 70f, Align.RIGHT),
            TableColumn("Shop & Status", 114f, Align.LEFT),
        )
        drawTable(
            context = context,
            columns = matColumns,
            rows = data.materials.mapIndexed { index, m ->
                val unitStr = when (m.unit) {
                    com.gharkharcha.data.entity.MaterialUnit.CUSTOM -> m.customUnit
                    null -> ""
                    else -> m.unit.name.lowercase()
                }
                val qtyStr = if (m.quantityMilli != null && m.quantityMilli > 0) {
                    "${Quantity.format(m.quantityMilli)} $unitStr".trim()
                } else "-"
                val itemStr = if (m.brand.isNotBlank()) "${m.itemName} (${m.brand})" else m.itemName
                val statusStr = if (m.isPaid) m.shopName else "${m.shopName} [Unpaid]"
                listOf(
                    (index + 1).toString(),
                    DateFormats.formatCompact(m.purchaseDate),
                    itemStr,
                    qtyStr,
                    if (m.ratePaise != null && m.ratePaise > 0) Money.format(m.ratePaise) else "-",
                    Money.format(m.totalPaise),
                    statusStr,
                )
            },
            totalRow = listOf("Subtotal", "", "", "", "", Money.format(data.totalMaterialPaise), ""),
        )

        // 2. Labour Attendance Table
        context.ensureSpace(60f)
        drawSectionHeader(context, "2. LABOUR ATTENDANCE (${data.attendance.size})")
        val attColumns = listOf(
            TableColumn("S.No", 28f, Align.CENTER),
            TableColumn("Date", 56f, Align.LEFT),
            TableColumn("Labourer", 125f, Align.LEFT),
            TableColumn("Skill", 70f, Align.LEFT),
            TableColumn("Hours", 44f, Align.RIGHT),
            TableColumn("Pay Amount", 80f, Align.RIGHT),
            TableColumn("Advance", 60f, Align.RIGHT),
            TableColumn("Status", 60f, Align.LEFT),
        )
        val totalAdvance = data.attendance.sumOf { it.attendance.advancePaise }
        drawTable(
            context = context,
            columns = attColumns,
            rows = data.attendance.mapIndexed { index, a ->
                val att = a.attendance
                val hours = String.format(Locale.US, "%.1f", att.minutesWorked / 60.0)
                listOf(
                    (index + 1).toString(),
                    DateFormats.formatCompact(att.date),
                    a.labourerName,
                    a.labourerSkill,
                    hours,
                    Money.format(att.totalPayPaise),
                    if (att.advancePaise > 0) Money.format(att.advancePaise) else "-",
                    att.status.name,
                )
            },
            totalRow = listOf(
                "Subtotal",
                "",
                "",
                "",
                "",
                Money.format(data.totalLabourPaise),
                if (totalAdvance > 0) Money.format(totalAdvance) else "-",
                "",
            ),
        )

        // 3. Payments Table
        if (data.payments.isNotEmpty()) {
            context.ensureSpace(60f)
            drawSectionHeader(context, "3. DIRECT PAYMENTS TO LABOURERS (${data.payments.size})")
            val payColumns = listOf(
                TableColumn("S.No", 28f, Align.CENTER),
                TableColumn("Date", 56f, Align.LEFT),
                TableColumn("Labourer", 130f, Align.LEFT),
                TableColumn("Amount", 75f, Align.RIGHT),
                TableColumn("Type / Mode", 94f, Align.LEFT),
                TableColumn("Reference / Notes", 140f, Align.LEFT),
            )
            val totalPayments = data.payments.sumOf { it.payment.amountPaise }
            drawTable(
                context = context,
                columns = payColumns,
                rows = data.payments.mapIndexed { index, p ->
                    val pay = p.payment
                    val notesStr = if (pay.reference.isNotBlank()) "${pay.reference} ${pay.notes}".trim() else pay.notes
                    listOf(
                        (index + 1).toString(),
                        DateFormats.formatCompact(pay.date),
                        p.labourerName,
                        Money.format(pay.amountPaise),
                        "${pay.type.name} / ${pay.mode.name}",
                        notesStr,
                    )
                },
                totalRow = listOf("Subtotal", "", "", Money.format(totalPayments), "", ""),
            )
        }

        // 4. Photo Appendix (2x3 grid per page)
        if (data.photoPaths.isNotEmpty()) {
            drawPhotoAppendix(context, data.photoPaths)
        }

        context.finishAll()
        document.writeTo(output)
        document.close()
    }

    fun generateLabourerStatement(data: LabourerStatementData, output: OutputStream) {
        val document = PdfDocument()
        val generatedAtText = data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US))
        val context = PdfPageContext(
            document = document,
            reportTitle = "Labourer Statement & Ledger",
            subtitle = data.labourer.name,
            generatedAtText = generatedAtText,
        )

        context.startNewPage()

        // Profile & Meta Header
        drawLabourerProfileHeader(
            context = context,
            labourer = data.labourer,
            photoPath = data.photoPath,
            projectName = data.project?.name ?: "All Projects",
            dateRange = data.dateFilterText,
            generatedAt = generatedAtText,
        )

        // KPI Cards
        val balanceLabel = if (data.netDuesPaise >= 0) "Pending Dues" else "Advance Balance"
        drawKpiCards(
            context = context,
            cards = listOf(
                Pair("Days Worked", String.format(Locale.US, "%.1f", data.totalDays)),
                Pair("Hours Worked", String.format(Locale.US, "%.1f", data.totalMinutes / 60.0)),
                Pair("Daily Wage", Money.format(data.labourer.dailyWagePaise)),
                Pair("Total Earned", Money.format(data.totalEarnedPaise)),
                Pair("Total Paid", Money.format(data.totalPaidPaise)),
                Pair(balanceLabel, Money.format(kotlin.math.abs(data.netDuesPaise))),
            ),
        )

        // 1. Attendance Table
        drawSectionHeader(context, "1. WORK & ATTENDANCE LOG (${data.attendance.size})")
        val attColumns = listOf(
            TableColumn("S.No", 28f, Align.CENTER),
            TableColumn("Date", 56f, Align.LEFT),
            TableColumn("Work", 140f, Align.LEFT),
            TableColumn("Status", 65f, Align.LEFT),
            TableColumn("Hours", 45f, Align.RIGHT),
            TableColumn("Earned Pay", 75f, Align.RIGHT),
            TableColumn("Advance", 54f, Align.RIGHT),
            TableColumn("Notes", 60f, Align.LEFT),
        )
        val totalAdvance = data.attendance.sumOf { it.attendance.advancePaise }
        drawTable(
            context = context,
            columns = attColumns,
            rows = data.attendance.mapIndexed { index, a ->
                val att = a.attendance
                val hours = String.format(Locale.US, "%.1f", att.minutesWorked / 60.0)
                listOf(
                    (index + 1).toString(),
                    DateFormats.formatCompact(att.date),
                    a.workName,
                    att.status.name,
                    hours,
                    Money.format(att.totalPayPaise),
                    if (att.advancePaise > 0) Money.format(att.advancePaise) else "-",
                    att.notes,
                )
            },
            totalRow = listOf(
                "Total",
                "",
                "",
                "",
                "",
                Money.format(data.totalEarnedPaise),
                if (totalAdvance > 0) Money.format(totalAdvance) else "-",
                "",
            ),
        )

        // 2. Payments Table
        if (data.payments.isNotEmpty()) {
            context.ensureSpace(60f)
            drawSectionHeader(context, "2. PAYMENTS & TRANSFERS (${data.payments.size})")
            val payColumns = listOf(
                TableColumn("S.No", 28f, Align.CENTER),
                TableColumn("Date", 56f, Align.LEFT),
                TableColumn("Amount", 80f, Align.RIGHT),
                TableColumn("Type", 60f, Align.LEFT),
                TableColumn("Mode", 65f, Align.LEFT),
                TableColumn("Reference / UTR", 114f, Align.LEFT),
                TableColumn("Notes", 120f, Align.LEFT),
            )
            drawTable(
                context = context,
                columns = payColumns,
                rows = data.payments.mapIndexed { index, p ->
                    listOf(
                        (index + 1).toString(),
                        DateFormats.formatCompact(p.date),
                        Money.format(p.amountPaise),
                        p.type.name,
                        p.mode.name,
                        p.reference,
                        p.notes,
                    )
                },
                totalRow = listOf("Total", "", Money.format(data.totalPaidPaise), "", "", "", ""),
            )
        }

        // Signature Section
        context.ensureSpace(90f)
        drawSignatureLines(context)

        context.finishAll()
        document.writeTo(output)
        document.close()
    }

    // --- Drawing Components ---

    private fun drawReportMetaHeader(
        context: PdfPageContext,
        title: String,
        address: String,
        dateRange: String,
        generatedAt: String,
    ) {
        val canvas = context.currentCanvas ?: return
        val paintTitle = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSubtitle = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText(title, MARGIN_LEFT, context.y + 16f, paintTitle)
        if (address.isNotBlank()) {
            canvas.drawText(address, MARGIN_LEFT, context.y + 29f, paintSubtitle)
        }
        val rightX = MARGIN_LEFT + CONTENT_WIDTH
        val dateRangeText = "Period: $dateRange"
        val genText = "Generated: $generatedAt"
        val drWidth = paintSubtitle.measureText(dateRangeText)
        val genWidth = paintSubtitle.measureText(genText)

        canvas.drawText(dateRangeText, rightX - drWidth, context.y + 15f, paintSubtitle)
        canvas.drawText(genText, rightX - genWidth, context.y + 28f, paintSubtitle)

        context.y += 38f
    }

    private fun drawLabourerProfileHeader(
        context: PdfPageContext,
        labourer: com.gharkharcha.data.entity.LabourerEntity,
        photoPath: String?,
        projectName: String,
        dateRange: String,
        generatedAt: String,
    ) {
        val canvas = context.currentCanvas ?: return

        var photoDrawn = false
        if (!photoPath.isNullOrBlank()) {
            val file = File(photoPath)
            if (file.exists()) {
                val bitmap = decodeSampledBitmap(file.absolutePath, 48, 48)
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, null, RectF(MARGIN_LEFT, context.y + 2f, MARGIN_LEFT + 46f, context.y + 48f), null)
                    photoDrawn = true
                }
            }
        }

        val textStartX = if (photoDrawn) MARGIN_LEFT + 56f else MARGIN_LEFT
        val paintName = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDetails = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            isAntiAlias = true
        }

        canvas.drawText(labourer.name, textStartX, context.y + 15f, paintName)
        val skillPhone = listOfNotNull(
            labourer.customSkill.ifBlank { null },
            labourer.phone.ifBlank { null },
            "Daily: ${Money.format(labourer.dailyWagePaise)}",
        ).joinToString("  •  ")
        canvas.drawText(skillPhone, textStartX, context.y + 29f, paintDetails)
        canvas.drawText("House: $projectName", textStartX, context.y + 42f, paintDetails)

        val rightX = MARGIN_LEFT + CONTENT_WIDTH
        val drText = "Period: $dateRange"
        val genText = "Generated: $generatedAt"
        val drWidth = paintDetails.measureText(drText)
        val genWidth = paintDetails.measureText(genText)
        canvas.drawText(drText, rightX - drWidth, context.y + 15f, paintDetails)
        canvas.drawText(genText, rightX - genWidth, context.y + 29f, paintDetails)

        context.y += 54f
    }

    private fun drawKpiCards(context: PdfPageContext, cards: List<Pair<String, String>>) {
        val canvas = context.currentCanvas ?: return
        val cardMargin = 6f
        val numCols = 3
        val cardWidth = (CONTENT_WIDTH - (numCols - 1) * cardMargin) / numCols
        val cardHeight = 36f

        val bgPaint = Paint().apply {
            color = COLOR_PRIMARY_CONTAINER
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        val labelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        cards.forEachIndexed { index, (label, value) ->
            val col = index % numCols
            val row = index / numCols
            val x = MARGIN_LEFT + col * (cardWidth + cardMargin)
            val y = context.y + row * (cardHeight + cardMargin)

            val rect = RectF(x, y, x + cardWidth, y + cardHeight)
            canvas.drawRoundRect(rect, 4f, 4f, bgPaint)
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

            canvas.drawText(label, x + 8f, y + 13f, labelPaint)
            canvas.drawText(value, x + 8f, y + 29f, valuePaint)
        }

        val totalRows = (cards.size + numCols - 1) / numCols
        context.y += totalRows * (cardHeight + cardMargin) + 6f
    }

    private fun drawSectionHeader(context: PdfPageContext, title: String) {
        val canvas = context.currentCanvas ?: return
        val paint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, MARGIN_LEFT, context.y + 14f, paint)

        val linePaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN_LEFT, context.y + 18f, MARGIN_LEFT + CONTENT_WIDTH, context.y + 18f, linePaint)
        context.y += 24f
    }

    private enum class Align { LEFT, CENTER, RIGHT }
    private data class TableColumn(val title: String, val width: Float, val align: Align)

    private fun drawTable(
        context: PdfPageContext,
        columns: List<TableColumn>,
        rows: List<List<String>>,
        totalRow: List<String>? = null,
    ) {
        val rowHeight = 18f
        val headerHeight = 20f

        val headerBgPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color = COLOR_WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val zebraBgPaint = Paint().apply {
            color = COLOR_ZEBRA
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val cellTextPaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 8f
            isAntiAlias = true
        }
        val totalTextPaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawHeaderRow(canvas: Canvas) {
            canvas.drawRect(MARGIN_LEFT, context.y, MARGIN_LEFT + CONTENT_WIDTH, context.y + headerHeight, headerBgPaint)
            var currentX = MARGIN_LEFT
            columns.forEach { col ->
                val textX = when (col.align) {
                    Align.LEFT -> currentX + 4f
                    Align.CENTER -> currentX + col.width / 2f - headerTextPaint.measureText(col.title) / 2f
                    Align.RIGHT -> currentX + col.width - 4f - headerTextPaint.measureText(col.title)
                }
                canvas.drawText(col.title, textX, context.y + 13.5f, headerTextPaint)
                currentX += col.width
            }
            context.y += headerHeight
        }

        // Draw initial header
        var canvas = context.ensureSpace(headerHeight + rowHeight)
        drawHeaderRow(canvas)

        // Draw data rows
        rows.forEachIndexed { rowIndex, row ->
            canvas = context.ensureSpace(rowHeight) {
                drawHeaderRow(context.currentCanvas!!)
            }

            if (rowIndex % 2 == 1) {
                canvas.drawRect(MARGIN_LEFT, context.y, MARGIN_LEFT + CONTENT_WIDTH, context.y + rowHeight, zebraBgPaint)
            }
            canvas.drawLine(MARGIN_LEFT, context.y + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, context.y + rowHeight, borderPaint)

            var currentX = MARGIN_LEFT
            row.forEachIndexed { colIndex, cell ->
                if (colIndex < columns.size) {
                    val col = columns[colIndex]
                    val maxTextWidth = col.width - 8f
                    val displayCell = truncateText(cellTextPaint, cell, maxTextWidth)
                    val textX = when (col.align) {
                        Align.LEFT -> currentX + 4f
                        Align.CENTER -> currentX + col.width / 2f - cellTextPaint.measureText(displayCell) / 2f
                        Align.RIGHT -> currentX + col.width - 4f - cellTextPaint.measureText(displayCell)
                    }
                    canvas.drawText(displayCell, textX, context.y + 12.5f, cellTextPaint)
                    currentX += col.width
                }
            }
            context.y += rowHeight
        }

        // Draw optional total row
        if (totalRow != null) {
            canvas = context.ensureSpace(rowHeight) {
                drawHeaderRow(context.currentCanvas!!)
            }

            val totalBgPaint = Paint().apply {
                color = COLOR_PRIMARY_CONTAINER
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN_LEFT, context.y, MARGIN_LEFT + CONTENT_WIDTH, context.y + rowHeight, totalBgPaint)
            canvas.drawLine(MARGIN_LEFT, context.y + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, context.y + rowHeight, borderPaint)

            var currentX = MARGIN_LEFT
            totalRow.forEachIndexed { colIndex, cell ->
                if (colIndex < columns.size) {
                    val col = columns[colIndex]
                    val maxTextWidth = col.width - 8f
                    val displayCell = truncateText(totalTextPaint, cell, maxTextWidth)
                    val textX = when (col.align) {
                        Align.LEFT -> currentX + 4f
                        Align.CENTER -> currentX + col.width / 2f - totalTextPaint.measureText(displayCell) / 2f
                        Align.RIGHT -> currentX + col.width - 4f - totalTextPaint.measureText(displayCell)
                    }
                    canvas.drawText(displayCell, textX, context.y + 12.5f, totalTextPaint)
                    currentX += col.width
                }
            }
            context.y += rowHeight
        }

        context.y += 12f
    }

    private fun drawSignatureLines(context: PdfPageContext) {
        val canvas = context.currentCanvas ?: return
        val linePaint = Paint().apply {
            color = COLOR_TEXT_DARK
            strokeWidth = 1f
        }
        val labelPaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sigWidth = 190f
        val leftX = MARGIN_LEFT + 20f
        val rightX = MARGIN_LEFT + CONTENT_WIDTH - sigWidth - 20f
        val lineY = context.y + 40f

        canvas.drawLine(leftX, lineY, leftX + sigWidth, lineY, linePaint)
        canvas.drawText("Labourer's Signature", leftX + 35f, lineY + 14f, labelPaint)

        canvas.drawLine(rightX, lineY, rightX + sigWidth, lineY, linePaint)
        canvas.drawText("Contractor / Owner Signature", rightX + 20f, lineY + 14f, labelPaint)

        context.y += 65f
    }

    private fun drawPhotoAppendix(context: PdfPageContext, photoPaths: List<String>) {
        context.startNewPage()
        drawSectionHeader(context, "PHOTO APPENDIX - BILLS & PROOFS (${photoPaths.size})")

        val cols = 2
        val rowsPerPage = 3
        val spacing = 12f
        val photoWidth = (CONTENT_WIDTH - spacing) / cols
        val photoHeight = 175f

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val bgPaint = Paint().apply {
            color = COLOR_ZEBRA
            style = Paint.Style.FILL
        }
        val captionPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
            isAntiAlias = true
        }

        photoPaths.forEachIndexed { index, path ->
            val pageIndex = index / (cols * rowsPerPage)
            val indexOnPage = index % (cols * rowsPerPage)
            if (indexOnPage == 0 && index > 0) {
                context.startNewPage()
                drawSectionHeader(context, "PHOTO APPENDIX (CONTINUED)")
            }

            val canvas = context.currentCanvas ?: return@forEachIndexed
            val col = indexOnPage % cols
            val row = indexOnPage / cols

            val x = MARGIN_LEFT + col * (photoWidth + spacing)
            val y = context.y + row * (photoHeight + spacing + 14f)

            val rect = RectF(x, y, x + photoWidth, y + photoHeight)
            canvas.drawRoundRect(rect, 4f, 4f, bgPaint)
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

            val file = File(path)
            if (file.exists()) {
                val bitmap = decodeSampledBitmap(file.absolutePath, photoWidth.toInt(), photoHeight.toInt())
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, null, rect, null)
                }
            }
            val caption = "Photo ${index + 1}: ${file.name}"
            canvas.drawText(caption, x + 4f, y + photoHeight + 11f, captionPaint)
        }
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(filePath, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
