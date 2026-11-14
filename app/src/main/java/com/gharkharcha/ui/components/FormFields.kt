package com.gharkharcha.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.gharkharcha.R
import com.gharkharcha.util.DateFormats
import java.time.LocalDate

/** Standard single-line / multi-line text field with an inline error. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    ),
    prefix: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    autoFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    // Focus only on first appearance, not after rotation / returning to the screen.
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    if (autoFocus && !focusedOnce) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            focusedOnce = true
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        prefix = prefix,
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

/** Rupee amount; accepts digits and one decimal point. */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
) {
    AppTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            if (filtered.count { it == '.' } <= 1) onValueChange(filtered)
        },
        label = label,
        modifier = modifier,
        error = error,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        prefix = { Text(stringResource(R.string.currency_symbol)) },
    )
}

/** Read-only field that opens a Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
    error: String? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) showPicker = true }

    OutlinedTextField(
        value = date?.let(DateFormats::format) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.no_date)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = stringResource(R.string.select_date))
            }
        },
        interactionSource = interaction,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(state.selectedDateMillis?.let(::utcMillisToLocalDate))
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                if (allowClear && date != null) {
                    TextButton(onClick = {
                        onDateChange(null)
                        showPicker = false
                    }) { Text(stringResource(R.string.action_clear)) }
                } else {
                    TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Dropdown picker for a fixed list of options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private const val DAY_MILLIS = 86_400_000L

/** Material date pickers work in UTC midnight millis. */
fun LocalDate.toUtcMillis(): Long = toEpochDay() * DAY_MILLIS

fun utcMillisToLocalDate(millis: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(millis, DAY_MILLIS))
