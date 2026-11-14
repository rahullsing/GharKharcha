package com.gharkharcha.data.reports

import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

object CsvReportGenerator {

    private fun escape(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    private fun writeRow(writer: OutputStreamWriter, vararg cells: Any?) {
        val line = cells.joinToString(",") { escape(it) }
        writer.write(line)
        writer.write("\r\n")
    }

    private fun writeEmptyRow(writer: OutputStreamWriter) {
        writer.write("\r\n")
    }

    fun generateHouseSummary(data: HouseSummaryReportData, output: OutputStream) {
        val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
        // UTF-8 BOM for Microsoft Excel compatibility
        writer.write("\uFEFF")

        writeRow(writer, "GHARKHARCHA - HOUSE SUMMARY REPORT")
        writeRow(writer, "House Name", data.project.name)
        if (data.project.address.isNotBlank()) {
            writeRow(writer, "Address", data.project.address)
        }
        writeRow(writer, "Date Range", data.dateFilterText)
        writeRow(writer, "Generated At", data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
        writeEmptyRow(writer)

        // Summary Totals
        writeRow(writer, "EXECUTIVE SUMMARY")
        writeRow(writer, "Total Budget", Money.format(data.totalBudgetPaise))
        writeRow(writer, "Grand Total Spent", Money.format(data.grandTotalPaise))
        writeRow(writer, "Remaining Balance", Money.format(data.balancePaise))
        writeRow(writer, "Materials Spent", Money.format(data.totalMaterialPaise))
        writeRow(writer, "Labour Spent", Money.format(data.totalLabourPaise))
        writeRow(writer, "Unpaid to Shops", Money.format(data.unpaidShopPaise))
        writeEmptyRow(writer)

        // Breakdown by Work
        writeRow(writer, "BREAKDOWN BY WORK")
        writeRow(writer, "S.No", "Work Name", "Status", "Budget", "Materials", "Labour", "Total Spent", "% Used")
        data.works.forEachIndexed { index, row ->
            writeRow(
                writer,
                index + 1,
                row.work.name,
                row.work.status.name,
                Money.format(row.budgetPaise),
                Money.format(row.materialPaise),
                Money.format(row.labourPaise),
                Money.format(row.totalPaise),
                "${row.percentUsed}%",
            )
        }
        writeRow(
            writer,
            "Total",
            "",
            "",
            Money.format(data.totalBudgetPaise),
            Money.format(data.totalMaterialPaise),
            Money.format(data.totalLabourPaise),
            Money.format(data.grandTotalPaise),
            if (data.totalBudgetPaise > 0) "${((data.grandTotalPaise * 100) / data.totalBudgetPaise).toInt()}%" else "-",
        )
        writeEmptyRow(writer)

        // Monthly Breakdown
        if (data.monthlyTotals.isNotEmpty()) {
            writeRow(writer, "MONTHLY SPEND BREAKDOWN")
            writeRow(writer, "Month", "Materials", "Labour", "Total Spent")
            data.monthlyTotals.forEach { month ->
                writeRow(
                    writer,
                    month.yearMonth,
                    Money.format(month.materialPaise),
                    Money.format(month.labourPaise),
                    Money.format(month.totalPaise),
                )
            }
            writeRow(
                writer,
                "Total",
                Money.format(data.totalMaterialPaise),
                Money.format(data.totalLabourPaise),
                Money.format(data.grandTotalPaise),
            )
        }

        writer.flush()
    }

    fun generateWorkReport(data: WorkReportData, output: OutputStream) {
        val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
        writer.write("\uFEFF")

        writeRow(writer, "GHARKHARCHA - WORK DETAILED REPORT")
        writeRow(writer, "House Name", data.project.name)
        writeRow(writer, "Work Name", data.work.name)
        writeRow(writer, "Status", data.work.status.name)
        writeRow(writer, "Date Range", data.dateFilterText)
        writeRow(writer, "Generated At", data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
        writeEmptyRow(writer)

        // Financial Summary
        writeRow(writer, "FINANCIAL SUMMARY")
        writeRow(writer, "Work Budget", Money.format(data.budgetPaise))
        writeRow(writer, "Grand Total Spent", Money.format(data.grandTotalPaise))
        writeRow(
            writer,
            if (data.variancePaise >= 0) "Under Budget Variance" else "Over Budget Variance",
            Money.format(data.variancePaise),
        )
        writeRow(writer, "Total Materials Cost", Money.format(data.totalMaterialPaise))
        writeRow(writer, "Total Labour Cost", Money.format(data.totalLabourPaise))
        writeEmptyRow(writer)

        // Materials Table
        writeRow(writer, "1. MATERIALS PURCHASED")
        writeRow(writer, "S.No", "Date", "Item", "Brand", "Quantity", "Unit", "Rate", "Total Amount", "Shop / Vendor", "Paid Status")
        data.materials.forEachIndexed { index, m ->
            val qty = m.quantityMilli
            val rate = m.ratePaise
            val unitStr = when (m.unit) {
                com.gharkharcha.data.entity.MaterialUnit.CUSTOM -> m.customUnit.ifBlank { "-" }
                null -> "-"
                else -> m.unit.name.lowercase()
            }
            writeRow(
                writer,
                index + 1,
                DateFormats.format(m.purchaseDate),
                m.itemName,
                m.brand,
                if (qty != null && qty > 0) Quantity.format(qty) else "-",
                unitStr,
                if (rate != null && rate > 0) Money.format(rate) else "-",
                Money.format(m.totalPaise),
                m.shopName,
                if (m.isPaid) "Paid (${m.paymentMode})" else "Unpaid",
            )
        }
        writeRow(writer, "Materials Subtotal", "", "", "", "", "", "", Money.format(data.totalMaterialPaise), "", "")
        writeEmptyRow(writer)

        // Labour Attendance Table
        writeRow(writer, "2. LABOUR ATTENDANCE")
        writeRow(writer, "S.No", "Date", "Labourer Name", "Skill", "Status", "Hours Worked", "Overtime (min)", "Daily Wage", "Pay Amount", "Advance Given")
        data.attendance.forEachIndexed { index, a ->
            val att = a.attendance
            val hours = String.format("%.1f", att.minutesWorked / 60.0)
            writeRow(
                writer,
                index + 1,
                DateFormats.format(att.date),
                a.labourerName,
                a.labourerSkill,
                att.status.name,
                hours,
                att.overtimeMinutes,
                Money.format(att.ratePaise),
                Money.format(att.totalPayPaise),
                if (att.advancePaise > 0) Money.format(att.advancePaise) else "-",
            )
        }
        val totalAdvance = data.attendance.sumOf { it.attendance.advancePaise }
        writeRow(
            writer,
            "Labour Subtotal",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            Money.format(data.totalLabourPaise),
            if (totalAdvance > 0) Money.format(totalAdvance) else "-",
        )
        writeEmptyRow(writer)

        // Payments Table
        writeRow(writer, "3. DIRECT PAYMENTS TO LABOURERS")
        writeRow(writer, "S.No", "Date", "Labourer Name", "Amount", "Type", "Paid By", "Reference / UTR", "Notes")
        var totalPaymentsPaise = 0L
        data.payments.forEachIndexed { index, p ->
            val pay = p.payment
            totalPaymentsPaise += pay.amountPaise
            writeRow(
                writer,
                index + 1,
                DateFormats.format(pay.date),
                p.labourerName,
                Money.format(pay.amountPaise),
                pay.type.name,
                pay.mode.name,
                pay.reference,
                pay.notes,
            )
        }
        writeRow(writer, "Payments Subtotal", "", "", Money.format(totalPaymentsPaise), "", "", "", "")
        writeEmptyRow(writer)

        writer.flush()
    }

    fun generateLabourerStatement(data: LabourerStatementData, output: OutputStream) {
        val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
        writer.write("\uFEFF")

        writeRow(writer, "GHARKHARCHA - LABOURER STATEMENT")
        writeRow(writer, "Labourer Name", data.labourer.name)
        if (data.labourer.phone.isNotBlank()) {
            writeRow(writer, "Phone Number", data.labourer.phone)
        }
        writeRow(writer, "Role / Skill", data.labourer.customSkill)
        writeRow(writer, "Standard Daily Wage", Money.format(data.labourer.dailyWagePaise))
        if (data.project != null) {
            writeRow(writer, "House / Project", data.project.name)
        }
        writeRow(writer, "Date Range", data.dateFilterText)
        writeRow(writer, "Generated At", data.generatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
        writeEmptyRow(writer)

        // Summary metrics
        writeRow(writer, "STATEMENT SUMMARY")
        writeRow(writer, "Days Worked", String.format("%.1f", data.totalDays))
        writeRow(writer, "Hours Worked", String.format("%.1f", data.totalMinutes / 60.0))
        writeRow(writer, "Total Wages Earned", Money.format(data.totalEarnedPaise))
        writeRow(writer, "Total Payments Received", Money.format(data.totalPaidPaise))
        val balanceLabel = if (data.netDuesPaise >= 0) "Pending Due to Labourer" else "Advance Balance with Labourer"
        writeRow(writer, balanceLabel, Money.format(kotlin.math.abs(data.netDuesPaise)))
        writeEmptyRow(writer)

        // Attendance records
        writeRow(writer, "1. WORK & ATTENDANCE LOG")
        writeRow(writer, "S.No", "Date", "Work Name", "Status", "Hours Worked", "Overtime (min)", "Earned Pay", "Advance Given", "Notes")
        data.attendance.forEachIndexed { index, a ->
            val att = a.attendance
            val hours = String.format("%.1f", att.minutesWorked / 60.0)
            writeRow(
                writer,
                index + 1,
                DateFormats.format(att.date),
                a.workName,
                att.status.name,
                hours,
                att.overtimeMinutes,
                Money.format(att.totalPayPaise),
                if (att.advancePaise > 0) Money.format(att.advancePaise) else "-",
                att.notes,
            )
        }
        writeRow(writer, "Attendance Totals", "", "", "", "", "", Money.format(data.totalEarnedPaise), "", "")
        writeEmptyRow(writer)

        // Payments records
        writeRow(writer, "2. PAYMENTS & TRANSFERS")
        writeRow(writer, "S.No", "Date", "Amount", "Type", "Mode", "Reference / UTR", "Notes")
        data.payments.forEachIndexed { index, p ->
            writeRow(
                writer,
                index + 1,
                DateFormats.format(p.date),
                Money.format(p.amountPaise),
                p.type.name,
                p.mode.name,
                p.reference,
                p.notes,
            )
        }
        writeRow(writer, "Payments Totals", "", Money.format(data.totalPaidPaise), "", "", "", "")
        writeEmptyRow(writer)

        writer.flush()
    }
}
