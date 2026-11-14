package com.gharkharcha.ui.common

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** rememberSaveable support for nullable @Serializable UI state (e.g. an open bottom sheet). */
fun <T : Any> nullableJsonSaver(serializer: KSerializer<T>): Saver<T?, String> = Saver(
    save = { value -> value?.let { Json.encodeToString(serializer, it) } },
    restore = { json -> runCatching { Json.decodeFromString(serializer, json) }.getOrNull() },
)
