package com.example.film_app_mobile.feature.deck.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import com.example.film_app_mobile.components.DecisionControls
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.ui.model.Film
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
internal fun DeckCarousel(
    cards: List<Film>,
    busy: Boolean,
    modifier: Modifier,
    downwardProgress: (Float) -> Unit = {},
    progress: (Float) -> Unit = {},
    openDetail: ((Long) -> Unit)? = null,
    type: ContentType = ContentType.FILM,
    choose: (Film, String, () -> Unit) -> Unit,
) {
    val current = cards.first()
    val scope = key(current.id) { rememberCoroutineScope() }
    val latestBusy by rememberUpdatedState(busy)
    var position by remember(current.id) { mutableFloatStateOf(0f) }
    var vertical by remember(current.id) { mutableStateOf(false) }
    SideEffect {
        progress(if (vertical) 0f else position)
        downwardProgress(if (vertical) position else 0f)
    }
    var settling by remember(current.id) { mutableStateOf(false) }
    val enabled = !busy && !settling
    val commit by rememberUpdatedState(choose)
    fun settle(target: Float, decision: String?) {
        if (!enabled) return
        if (decision != null) vertical = decision == "WATCHED"
        settling = true
        scope.launch {
            animate(position, target, animationSpec = tween(220)) { value, _ -> position = value }
            if (decision == null || latestBusy) {
                position = 0f
                settling = false
            } else
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
        modifier
            .background(
                androidx.compose.ui.graphics.lerp(
                    AppTheme.colors.background,
                    if (vertical) AppTheme.colors.watched
                    else if (position > 0) AppTheme.colors.swipeLike
                    else AppTheme.colors.swipeDislike,
                    (abs(position) * 2).coerceAtMost(1f),
                )
            )
            .clipToBounds()
            .pointerInput(current.id, enabled) {
                if (enabled) {
                    try {
                        trackDeckSwipe(
                            move = { down, fraction ->
                                vertical = down
                                position = fraction
                            },
                            release = { decision ->
                                val target =
                                    when (decision) {
                                        null -> 0f
                                        "WONT_WATCH" -> -1f
                                        else -> 1f
                                    }
                                turn(target, decision)
                            },
                        )
                    } finally {
                        if (!settling) position = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val visible = cards.take(3)
        for (film in visible) key(film.id) {
            val slot =
                when {
                    film.id == current.id -> 0
                    cards.size == 2 -> if (position > 0f) -1 else 1
                    film.id == cards[1].id -> 1
                    else -> -1
                }
            val distance = (slot + if (vertical) 0f else position).coerceIn(-1.5f, 1.5f)
            val depth = abs(distance).coerceAtMost(1f)
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth(.98f)
                    .zIndex(1f - depth)
                    .graphicsLayer {
                        translationX = size.width * sin(distance * 1.1f)
                        translationY =
                            if (vertical && slot == 0) size.height * position
                            else size.height * .06f * depth * depth
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
                DeckPoster(film, type, depth, openDetail)
                if (slot == 0 && position != 0f)
                    Spacer(
                        Modifier.matchParentSize()
                            .background(
                                (if (vertical) AppTheme.colors.watched
                                    else if (position > 0f) AppTheme.colors.swipeLike
                                    else AppTheme.colors.swipeDislike)
                                    .copy(alpha = abs(position).coerceAtMost(1f) * .24f)
                            )
                    )
            }
        }
        if (abs(position) > .025f)
            AppText(
                stringResource(
                    if (vertical) R.string.watched
                    else if (position > 0) R.string.like_gesture else R.string.dislike_gesture
                ),
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .zIndex(3f)
                    .graphicsLayer { rotationZ = if (position > 0) -25f else 25f }
                    .padding(12.dp),
                color =
                    if (vertical) AppTheme.colors.watched
                    else if (position > 0) AppTheme.colors.swipeLike
                    else AppTheme.colors.swipeDislike,
                style = AppTheme.typography.title,
            )
    }
    DecisionControls(enabled, progress = if (vertical) 0f else position, large = true) { status ->
        settle(if (status == "WONT_WATCH") -1f else 1f, status)
    }
}
