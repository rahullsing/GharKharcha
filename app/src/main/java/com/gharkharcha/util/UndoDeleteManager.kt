package com.gharkharcha.util

import com.gharkharcha.di.ApplicationScope
import com.gharkharcha.ui.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delete-with-undo. Screens hide items whose key is in [pending] immediately; the real delete runs
 * only after the snackbar times out. Commit runs in the application scope so leaving the screen
 * doesn't cancel it. If the process dies during the window nothing is deleted (safe default).
 */
@Singleton
class UndoDeleteManager @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    data class Request(val key: String, val message: UiText, val commit: suspend () -> Unit)

    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending: StateFlow<Set<String>> = _pending.asStateFlow()

    private val requests = Channel<Request>(Channel.UNLIMITED)
    val requestFlow: Flow<Request> = requests.receiveAsFlow()

    fun schedule(key: String, message: UiText, commit: suspend () -> Unit) {
        _pending.update { it + key }
        requests.trySend(Request(key, message, commit))
    }

    fun undo(request: Request) {
        _pending.update { it - request.key }
    }

    /** Runs the delete. A failure must not crash the app: the item simply reappears. */
    fun commit(request: Request) {
        scope.launch {
            try {
                request.commit()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Row stays in the DB; clearing pending below makes it visible again.
            } finally {
                _pending.update { it - request.key }
            }
        }
    }

    companion object {
        fun projectKey(id: Long) = "project:$id"
        fun workKey(id: Long) = "work:$id"
        fun labourerKey(id: Long) = "labourer:$id"
        fun paymentKey(id: Long) = "payment:$id"
    }
}
