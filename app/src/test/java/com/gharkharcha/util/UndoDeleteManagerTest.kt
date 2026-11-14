package com.gharkharcha.util

import com.gharkharcha.ui.common.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoDeleteManagerTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val manager = UndoDeleteManager(scope)

    @Test fun `schedule hides item until undo, and undo never deletes`() = runTest(dispatcher) {
        var deleted = 0
        manager.schedule("work:1", UiText.Raw("x")) { deleted++ }
        assertEquals(setOf("work:1"), manager.pending.value)

        val request = manager.requestFlow.first()
        manager.undo(request)
        scope.advanceUntilIdle()

        assertTrue(manager.pending.value.isEmpty())
        assertEquals(0, deleted)
    }

    @Test fun `commit deletes once and clears pending`() = runTest(dispatcher) {
        var deleted = 0
        manager.schedule("work:2", UiText.Raw("x")) { deleted++ }
        manager.commit(manager.requestFlow.first())
        scope.advanceUntilIdle()

        assertEquals(1, deleted)
        assertTrue(manager.pending.value.isEmpty())
    }

    @Test fun `failed commit does not crash and the item reappears`() = runTest(dispatcher) {
        manager.schedule("work:3", UiText.Raw("x")) { error("db failure") }
        manager.commit(manager.requestFlow.first())
        scope.advanceUntilIdle() // would throw if the exception escaped
        assertTrue(manager.pending.value.isEmpty())
    }
}
