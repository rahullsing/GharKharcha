package com.gharkharcha.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.prefs.ThemeMode
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.LocalSnackbarHostState
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHostState.current
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Success -> snackbarHost.showSnackbar(event.message)
                is SettingsEvent.Error -> snackbarHost.showSnackbar(event.message)
            }
        }
    }

    // Permission launcher for reminders (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setAttendanceReminder(true, state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute)
        }
    }

    fun requestNotificationAndToggleAttendance(enabled: Boolean) {
        if (!enabled) {
            viewModel.setAttendanceReminder(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.setAttendanceReminder(true, state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setAttendanceReminder(true, state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute)
        }
    }

    // Backup SAF Create Document launcher
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            viewModel.createBackup(uri)
        }
    }

    // Restore SAF Open Document launcher
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    // Time picker dialog for attendance reminder
    var showTimePicker by remember { mutableStateOf(false) }

    DetailScaffold(
        topBar = { BackTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Appearance section
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeOption(
                            label = stringResource(R.string.settings_theme_system),
                            selected = state.prefs.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOption(
                            label = stringResource(R.string.settings_theme_light),
                            selected = state.prefs.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOption(
                            label = stringResource(R.string.settings_theme_dark),
                            selected = state.prefs.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_dynamic_color),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.settings_dynamic_color_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.prefs.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    }
                }
            }

            // Work & Currency Section
            SettingsSectionHeader(stringResource(R.string.settings_section_work_currency))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_hours_per_day),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.settings_hours_per_day_desc, state.prefs.hoursPerDay),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (state.prefs.hoursPerDay > 1) {
                                        viewModel.setHoursPerDay(state.prefs.hoursPerDay - 1)
                                    }
                                },
                                enabled = state.prefs.hoursPerDay > 1,
                            ) {
                                Icon(painterResource(R.drawable.ic_remove), contentDescription = "Decrease hours")
                            }
                            Text(
                                "${state.prefs.hoursPerDay}h",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            IconButton(
                                onClick = {
                                    if (state.prefs.hoursPerDay < 24) {
                                        viewModel.setHoursPerDay(state.prefs.hoursPerDay + 1)
                                    }
                                },
                                enabled = state.prefs.hoursPerDay < 24,
                            ) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = "Increase hours")
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_currency_symbol),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.settings_currency_symbol_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            state.prefs.currencySymbol,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Security Section
            SettingsSectionHeader(stringResource(R.string.settings_section_security))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_app_lock),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (state.isDeviceSecure) {
                                    stringResource(R.string.settings_app_lock_desc)
                                } else {
                                    stringResource(R.string.settings_app_lock_not_setup)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isDeviceSecure) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        Switch(
                            checked = state.prefs.appLockEnabled,
                            onCheckedChange = viewModel::setAppLockEnabled,
                            enabled = state.isDeviceSecure,
                        )
                    }
                }
            }

            // Reminders Section
            SettingsSectionHeader(stringResource(R.string.settings_section_reminders))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val formattedTime = remember(state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute) {
                            val t = LocalTime.of(state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute)
                            t.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        }
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_daily_attendance_reminder),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.settings_daily_attendance_reminder_desc, formattedTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.prefs.attendanceReminderEnabled,
                            onCheckedChange = ::requestNotificationAndToggleAttendance,
                        )
                    }

                    if (state.prefs.attendanceReminderEnabled) {
                        val formattedTime = remember(state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute) {
                            val t = LocalTime.of(state.prefs.attendanceReminderHour, state.prefs.attendanceReminderMinute)
                            t.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.settings_reminder_time),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(onClick = { showTimePicker = true }) {
                                Icon(painterResource(R.drawable.ic_schedule), contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(formattedTime)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.settings_payday_reminder),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.settings_payday_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.prefs.paydayReminderEnabled,
                            onCheckedChange = viewModel::setPaydayReminder,
                        )
                    }
                }
            }

            // Backup & Restore Section
            SettingsSectionHeader(stringResource(R.string.settings_section_backup))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val lastBackupText = remember(state.prefs.lastBackupTimestamp) {
                        if (state.prefs.lastBackupTimestamp <= 0) null
                        else {
                            val instant = Instant.ofEpochMilli(state.prefs.lastBackupTimestamp)
                            val zone = ZoneId.systemDefault()
                            DateTimeFormatter.ofPattern("dd-MMM-yyyy, hh:mm a").format(instant.atZone(zone))
                        }
                    }

                    Text(
                        if (lastBackupText != null) {
                            stringResource(R.string.settings_backup_last, lastBackupText)
                        } else {
                            stringResource(R.string.settings_backup_never)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Create backup item
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_backup_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_save), contentDescription = null)
                        },
                        trailingContent = {
                            if (state.isBackingUp) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = {
                                        val now = LocalDateTime.now()
                                        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(now)
                                        createBackupLauncher.launch("GharKharcha_Backup_$stamp.zip")
                                    },
                                    enabled = !state.isRestoring,
                                ) {
                                    Text(stringResource(R.string.settings_backup_action))
                                }
                            }
                        },
                    )

                    HorizontalDivider()

                    // Restore backup item
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_restore_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_restore_desc)) },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_restore), contentDescription = null)
                        },
                        trailingContent = {
                            if (state.isRestoring) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                    },
                                    enabled = !state.isBackingUp,
                                ) {
                                    Text(stringResource(R.string.settings_restore_action))
                                }
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.prefs.attendanceReminderHour,
            initialMinute = state.prefs.attendanceReminderMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    viewModel.setAttendanceReminder(
                        enabled = true,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                    )
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        ConfirmDialog(
            title = stringResource(R.string.settings_restore_confirm_title),
            message = stringResource(R.string.settings_restore_confirm_message),
            confirmLabel = stringResource(R.string.settings_restore_action),
            onConfirm = {
                showRestoreConfirmDialog = false
                val uri = pendingRestoreUri
                if (uri != null) {
                    viewModel.restoreBackup(uri)
                }
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            destructive = true,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        modifier = modifier,
    )
}
