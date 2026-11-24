package com.example.film_app_mobile.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun AppBottomSheet(dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Spacer(
                Modifier.matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.close_sheet),
                        onClick = dismiss,
                    )
            )
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(
                        max =
                            androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
                                .dp * 0.9f
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(c.surface)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(40.dp).pointerInput(Unit) {
                        var distance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { distance = 0f },
                            onDragEnd = { if (distance > 32.dp.toPx()) dismiss() },
                        ) { change, delta ->
                            change.consume()
                            distance += delta
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Spacer(
                        Modifier.size(36.dp, 4.dp)
                            .background(c.textSecondary, RoundedCornerShape(2.dp))
                    )
                }
                Column(
                    Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content()
                }
            }
        }
    }
}
