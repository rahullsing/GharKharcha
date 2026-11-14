package com.gharkharcha.ui.photos

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offset by mutableStateOf(Offset.Zero)
        internal set
    internal var size = IntSize.Zero

    val isZoomed: Boolean get() = scale > 1.01f

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    internal fun apply(zoom: Float, pan: Offset) {
        scale = (scale * zoom).coerceIn(MIN, MAX)
        offset = if (scale <= 1f) Offset.Zero else clamp(offset + pan)
    }

    internal fun toggle(at: Offset) {
        if (isZoomed) {
            reset()
        } else {
            scale = DOUBLE_TAP
            // Keep the tapped point roughly under the finger.
            val center = Offset(size.width / 2f, size.height / 2f)
            offset = clamp((center - at) * (DOUBLE_TAP - 1f))
        }
    }

    private fun clamp(o: Offset): Offset {
        val maxX = size.width * (scale - 1f) / 2f
        val maxY = size.height * (scale - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    private companion object {
        const val MIN = 1f
        const val MAX = 5f
        const val DOUBLE_TAP = 2.5f
    }
}

@Composable
fun rememberZoomState(): ZoomState = remember { ZoomState() }

/**
 * Pinch-zoom, pan when zoomed, double-tap to toggle. Single-finger drags are left alone while not
 * zoomed so a surrounding pager can still swipe.
 */
fun Modifier.zoomable(state: ZoomState): Modifier = this
    .onSizeChanged { state.size = it }
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val multiTouch = event.changes.count { it.pressed } > 1
                if (multiTouch || state.isZoomed) {
                    state.apply(event.calculateZoom(), event.calculatePan())
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(state) { detectTapGestures(onDoubleTap = state::toggle) }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }
