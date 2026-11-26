package com.example.film_app_mobile.feature.folder.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme

@Composable
internal fun RecommendationTitle(title: String) {
    val measurer = rememberTextMeasurer()
    val base = AppTheme.typography.title
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val (lines, style) =
            remember(title, width, measurer, base) {
                val words = title.trim().split(Regex("\\s+"))
                var size = base.fontSize.value * 2
                var rows: MutableList<String>
                var candidate: TextStyle
                do {
                    candidate = base.copy(fontSize = size.sp, lineHeight = (size * 1.12f).sp)
                    fun fits(line: String) =
                        measurer.measure(line, candidate, softWrap = false).size.width <= width

                    rows = mutableListOf()
                    var line = ""
                    for (word in words) {
                        val joined = if (line.isEmpty()) word else "$line $word"
                        if (line.isNotEmpty() && !fits(joined)) {
                            rows.add(line)
                            line = word
                        } else line = joined
                    }
                    rows.add(line)
                    val fitsTitle =
                        rows.size <= 2 &&
                            rows.all(::fits) &&
                            size * 1.12f * rows.size <= base.lineHeight.value * 2
                    size *= .9f
                } while (!fitsTitle && size >= .1f)
                rows.joinToString("\n") to candidate
            }
        AppText(lines, style = style, maxLines = 2)
    }
}
