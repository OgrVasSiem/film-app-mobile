package com.example.film_app_mobile.feature.deck

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.Link
import com.example.film_app_mobile.core.designsystem.component.Action
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.Film
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
internal fun DeckCarousel(
    cards: List<Film>,
    busy: Boolean,
    modifier: Modifier,
    choose: (Film, String, () -> Unit) -> Unit,
) {
    val current = cards.first()
    val scope = rememberCoroutineScope()
    var position by remember(current.id) { mutableFloatStateOf(0f) }
    var settling by remember(current.id) { mutableStateOf(false) }
    val enabled = !busy && !settling
    val commit by rememberUpdatedState(choose)
    fun settle(target: Float, decision: String?) {
        if (!enabled) return
        settling = true
        scope.launch {
            animate(position, target, animationSpec = tween(220)) { value, _ -> position = value }
            if (decision == null) settling = false
            else
                commit(current, decision) {
                    scope.launch {
                        animate(position, 0f, animationSpec = tween(200)) { value, _ ->
                            position = value
                        }
                        settling = false
                    }
                }
        }
    }
    val turn by
        rememberUpdatedState<(Float, String?) -> Unit> { target, decision ->
            settle(target, decision)
        }
    Box(
        modifier.clipToBounds().pointerInput(current.id, enabled) {
            if (enabled)
                detectHorizontalDragGestures(
                    onDragCancel = { turn(0f, null) },
                    onDragEnd = {
                        when {
                            position >= .22f -> turn(1f, "WILL_WATCH")
                            position <= -.22f -> turn(-1f, "WONT_WATCH")
                            else -> turn(0f, null)
                        }
                    },
                ) { change, delta ->
                    change.consume()
                    position = (position + delta / size.width.coerceAtLeast(1)).coerceIn(-1f, 1f)
                }
        },
        contentAlignment = Alignment.Center,
    ) {
        val visible =
            listOfNotNull(current, cards.getOrNull(1), cards.lastOrNull()).distinctBy { it.id }
        for (film in visible) key(film.id) {
            val slot =
                when {
                    film.id == current.id -> 0
                    cards.size == 2 -> if (position > 0f) -1 else 1
                    film.id == cards[1].id -> 1
                    else -> -1
                }
            val distance = (slot + position).coerceIn(-1.5f, 1.5f)
            val depth = abs(distance).coerceAtMost(1f)
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth(.91f)
                    .zIndex(1f - depth)
                    .graphicsLayer {
                        translationX = size.width * sin(distance * 1.1f)
                        translationY = size.height * .06f * depth * depth
                        rotationZ = distance * 9f
                        rotationY = -distance * 14f
                        scaleX = 1f - .16f * depth
                        scaleY = scaleX
                        alpha = 1f - .26f * depth
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .blur((depth * 12f).dp)
                    .background(AppTheme.colors.surface)
                    .then(if (slot == 0) Modifier else Modifier.clearAndSetSemantics {})
            ) {
                DeckPoster(film, depth)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            Link(stringResource(R.string.reject), enabled) { settle(-1f, "WONT_WATCH") }
        }
        Box(Modifier.weight(1f)) {
            Action(stringResource(R.string.accept), enabled) { settle(1f, "WILL_WATCH") }
        }
    }
    Link(stringResource(R.string.mark_watched), enabled) { settle(-1f, "WATCHED") }
}
