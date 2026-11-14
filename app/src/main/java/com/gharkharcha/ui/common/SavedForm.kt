package com.gharkharcha.ui.common

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Form state persisted in [SavedStateHandle] as JSON so edits survive rotation and process death.
 * [restored] tells the ViewModel not to overwrite user edits with a fresh DB load.
 */
class SavedForm<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    private val serializer: KSerializer<T>,
    initial: () -> T,
) {
    private val saved: T? = handle.get<String>(key)?.let { json ->
        runCatching { Json.decodeFromString(serializer, json) }.getOrNull()
    }

    val restored: Boolean = saved != null

    private val _state = MutableStateFlow(saved ?: initial())
    val state: StateFlow<T> = _state.asStateFlow()

    val value: T get() = _state.value

    fun update(transform: (T) -> T) {
        val next = transform(_state.value)
        _state.value = next
        handle[key] = Json.encodeToString(serializer, next)
    }
}
