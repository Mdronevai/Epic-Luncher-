package com.example.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.example.model.GestureType
import kotlin.math.abs

fun Modifier.launcherGestureHandler(
    onGesture: (GestureType) -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var zoomTotal = 1f
        var panYTotal = 0f
        var panXTotal = 0f
        var maxPointerCount = 1
        var isTwoFinger = false
        var gestureTriggered = false

        val downTime = System.currentTimeMillis()

        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val count = event.changes.size
            if (count > maxPointerCount) {
                maxPointerCount = count
            }
            if (count >= 2) {
                isTwoFinger = true
            }

            if (count >= 2 && !gestureTriggered) {
                val zoomChange = event.calculateZoom()
                zoomTotal *= zoomChange
                val panChange = event.calculatePan()
                panYTotal += panChange.y

                if (zoomTotal < 0.78f) {
                    onGesture(GestureType.PINCH_IN)
                    gestureTriggered = true
                    event.changes.forEach { it.consume() }
                } else if (zoomTotal > 1.25f) {
                    onGesture(GestureType.PINCH_OUT)
                    gestureTriggered = true
                    event.changes.forEach { it.consume() }
                } else if (abs(panYTotal) > 80f) {
                    if (panYTotal < -80f) {
                        onGesture(GestureType.TWO_FINGER_SWIPE_UP)
                    } else {
                        onGesture(GestureType.TWO_FINGER_SWIPE_DOWN)
                    }
                    gestureTriggered = true
                    event.changes.forEach { it.consume() }
                }
            } else if (count == 1 && !gestureTriggered) {
                val change = event.changes.first()
                val posChange = change.positionChange()
                panYTotal += posChange.y
                panXTotal += posChange.x

                // If swipe up/down exceeds vertical threshold and dominant over horizontal
                if (abs(panYTotal) > 130f && abs(panYTotal) > abs(panXTotal) * 1.5f) {
                    if (panYTotal < -130f) {
                        onGesture(GestureType.SWIPE_UP)
                    } else {
                        onGesture(GestureType.SWIPE_DOWN)
                    }
                    gestureTriggered = true
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        val duration = System.currentTimeMillis() - downTime

        // If tap was short and not a swipe, check for single/double tap or long press
        if (!gestureTriggered && abs(panYTotal) < 30f && abs(panXTotal) < 30f) {
            if (duration >= 500) {
                onLongPress()
            }
        }
    }
}
