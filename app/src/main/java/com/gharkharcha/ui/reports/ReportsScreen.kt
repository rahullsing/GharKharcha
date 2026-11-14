package com.gharkharcha.ui.reports

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.reports.DateRangePreset
import com.gharkharcha.data.reports.ReportFormat
import com.gharkharcha.data.reports.ReportType
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val ContentMaxWidth = 680.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = com.gharkharcha.ui.components.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val savedAndSharedText = stringResource(R.string.report_saved_and_shared)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReportsEvent.ReportGenerated -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.report.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.report.contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, event.report.fileName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Report")
                    context.startActivity(chooser)
                    scope.launch {
                        snackbarHostState.showSnackbar(savedAndSharedText)
                    }
                }
                is ReportsEvent.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    DetailScaffold(
        topBar = {
            BackTopBar(
                title = stringResource(R.string.reports_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ContentMaxWidth)
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 1. Report Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.report_type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val types = listOf(
                            Triple(ReportType.HOUSE_SUMMARY, stringResource(R.string.report_type_house), 0),
                            Triple(ReportType.WORK_REPORT, stringResource(R.string.report_type_work), 1),
                            Triple(ReportType.LABOURER_STATEMENT, stringResource(R.string.report_type_labourer), 2),
                        )
                        types.forEach { (type, label, index) ->
                            SegmentedButton(
                                selected = ui.reportType == type,
                                onClick = { viewModel.setReportType(type) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.report_format),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val formats = listOf(
                            Pair(ReportFormat.PDF, stringResource(R.string.report_format_pdf)),
                            Pair(ReportFormat.CSV, stringResource(R.string.report_format_csv)),
                        )
                        formats.forEachIndexed { index, (format, label) ->
                            SegmentedButton(
                                selected = ui.reportFormat == format,
                                onClick = { viewModel.setReportFormat(format) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                // 3. Conditional Target Dropdown
                if (ui.reportType == ReportType.WORK_REPORT) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.report_select_work),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        var expanded by remember { mutableStateOf(false) }
                        val selectedWork = ui.works.firstOrNull { it.id == ui.selectedWorkId }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = selectedWork?.name ?: stringResource(R.string.report_select_work),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                ui.works.forEach { work ->
                                    DropdownMenuItem(
                                        text = { Text("${work.name} (${work.status.name})") },
                                        onClick = {
                                            viewModel.setSelectedWorkId(work.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else if (ui.reportType == ReportType.LABOURER_STATEMENT) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.report_select_labourer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        var expanded by remember { mutableStateOf(false) }
                        val selectedLabourer = ui.labourers.firstOrNull { it.id == ui.selectedLabourerId }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = selectedLabourer?.name ?: stringResource(R.string.report_select_labourer),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                ui.labourers.forEach { labourer ->
                                    DropdownMenuItem(
                                        text = { Text("${labourer.name} (${labourer.customSkill})") },
                                        onClick = {
                                            viewModel.setSelectedLabourerId(labourer.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Date Range Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.report_filter_date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = ui.datePreset == DateRangePreset.ALL_TIME,
                            onClick = { viewModel.setDatePreset(DateRangePreset.ALL_TIME) },
                            label = { Text(stringResource(R.string.report_date_all_time)) },
                        )
                        FilterChip(
                            selected = ui.datePreset == DateRangePreset.THIS_MONTH,
                            onClick = { viewModel.setDatePreset(DateRangePreset.THIS_MONTH) },
                            label = { Text(stringResource(R.string.report_date_this_month)) },
                        )
                        FilterChip(
                            selected = ui.datePreset == DateRangePreset.LAST_MONTH,
                            onClick = { viewModel.setDatePreset(DateRangePreset.LAST_MONTH) },
                            label = { Text(stringResource(R.string.report_date_last_month)) },
                        )
                        FilterChip(
                            selected = ui.datePreset == DateRangePreset.CUSTOM,
                            onClick = { viewModel.setDatePreset(DateRangePreset.CUSTOM) },
                            label = { Text(stringResource(R.string.report_date_custom)) },
                        )
                    }

                    if (ui.datePreset == DateRangePreset.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DateField(
                                label = stringResource(R.string.report_from_date),
                                date = ui.customStartDate,
                                onDateChange = viewModel::setCustomStartDate,
                                allowClear = true,
                                modifier = Modifier.weight(1f),
                            )
                            DateField(
                                label = stringResource(R.string.report_to_date),
                                date = ui.customEndDate,
                                onDateChange = viewModel::setCustomEndDate,
                                allowClear = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 5. Additional Options
                if (ui.reportType == ReportType.WORK_REPORT && ui.reportFormat == ReportFormat.PDF) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setIncludePhotos(!ui.includePhotos) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Checkbox(
                                checked = ui.includePhotos,
                                onCheckedChange = viewModel::setIncludePhotos,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.report_include_photos),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // 6. Preview Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.report_preview_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (ui.reportFormat == ReportFormat.PDF) "PDF (A4)" else "CSV (Excel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        val scopeSummary = when (ui.reportType) {
                            ReportType.HOUSE_SUMMARY -> {
                                stringResource(
                                    R.string.report_preview_house_summary,
                                    ui.currentProject?.name ?: "House",
                                )
                            }
                            ReportType.WORK_REPORT -> {
                                val work = ui.works.firstOrNull { it.id == ui.selectedWorkId }
                                if (work != null) {
                                    "Work: ${work.name} · Budget: ${Money.format(work.budgetPaise)}"
                                } else {
                                    stringResource(R.string.report_select_work)
                                }
                            }
                            ReportType.LABOURER_STATEMENT -> {
                                val labourer = ui.labourers.firstOrNull { it.id == ui.selectedLabourerId }
                                if (labourer != null) {
                                    "Labourer: ${labourer.name} · Wage: ${Money.format(labourer.dailyWagePaise)}/day"
                                } else {
                                    stringResource(R.string.report_select_labourer)
                                }
                            }
                        }

                        Text(
                            text = scopeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )

                        Text(
                            text = "Date range: ${
                                when (ui.datePreset) {
                                    DateRangePreset.ALL_TIME -> "All time"
                                    DateRangePreset.THIS_MONTH -> "This month"
                                    DateRangePreset.LAST_MONTH -> "Last month"
                                    DateRangePreset.CUSTOM -> {
                                        val s = ui.customStartDate?.toString() ?: "Start"
                                        val e = ui.customEndDate?.toString() ?: "Now"
                                        "$s to $e"
                                    }
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 7. Action Button: Generate & Share
                val canGenerate = when (ui.reportType) {
                    ReportType.HOUSE_SUMMARY -> true
                    ReportType.WORK_REPORT -> ui.selectedWorkId != 0L
                    ReportType.LABOURER_STATEMENT -> ui.selectedLabourerId != 0L
                }

                Button(
                    onClick = viewModel::generateReport,
                    enabled = canGenerate && !ui.isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (ui.isGenerating) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.report_generating))
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.report_generate_share),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
