package com.gharkharcha.ui.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Loading / content / error for every screen. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
    data class Error(val cause: Throwable) : LoadState<Nothing>
}

/** Wraps a flow factory so an error state can be retried by bumping [retry]. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> retryableLoad(retry: MutableStateFlow<Int>, source: () -> Flow<T>): Flow<LoadState<T>> =
    retry.flatMapLatest {
        source()
            .map<T, LoadState<T>> { LoadState.Ready(it) }
            .catch { emit(LoadState.Error(it)) }
    }
