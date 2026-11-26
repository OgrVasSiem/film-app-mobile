package com.example.film_app_mobile.feature.deck.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal suspend fun PointerInputScope.trackDeckSwipe(
    move: (down: Boolean, fraction: Float) -> Unit,
    release: (String?) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val width = size.width.coerceAtLeast(1)
        val height = size.height.coerceAtLeast(1)
        val downThreshold = minOf(48.dp.toPx(), height * .18f)
        var dragging = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pointer = event.changes.firstOrNull { it.id == down.id }
            if (
                pointer == null ||
                    pointer.isConsumed ||
                    event.changes.any { it.id != down.id && it.pressed }
            ) {
                if (dragging) release(null)
                break
            }
            val travel = pointer.position - down.position
            if (!dragging && travel.getDistance() > viewConfiguration.touchSlop) dragging = true
            if (dragging) {
                // Keep taps available to the poster; own the rest of a drag, including its up.
                pointer.consume()
                val downward = travel.y > 0f && travel.y >= abs(travel.x) * .8f
                move(
                    downward,
                    if (downward) (travel.y / height).coerceIn(0f, 1f)
                    else (travel.x / width).coerceIn(-1f, 1f),
                )
                if (!pointer.pressed) {
                    release(
                        when {
                            downward && travel.y >= downThreshold -> "WATCHED"
                            !downward && travel.x >= width * .22f -> "WILL_WATCH"
                            !downward && travel.x <= -width * .22f -> "WONT_WATCH"
                            else -> null
                        }
                    )
                }
            }
        } while (pointer.pressed)
    }
}
