package com.example.film_app_mobile.feature.packs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.film_app_mobile.R
import com.example.film_app_mobile.components.*
import com.example.film_app_mobile.components.ScreenContent
import com.example.film_app_mobile.core.designsystem.component.AppText
import com.example.film_app_mobile.core.designsystem.theme.AppTheme
import com.example.film_app_mobile.ui.model.*
import kotlinx.coroutines.*

@Composable
internal fun PacksScreen(
    state: PacksState,
    busy: Boolean,
    queryChanged: (String) -> Unit,
    openPack: (Pack) -> Unit,
) {
    val c = AppTheme.colors
    val (packs, query) = state
    var premiumOffer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (premiumOffer) PremiumOfferSheet { premiumOffer = false }
    BasicTextField(
        query,
        queryChanged,
        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(12.dp)).padding(14.dp),
        singleLine = true,
        textStyle = AppTheme.typography.body.copy(color = c.textPrimary),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty())
                    AppText(stringResource(R.string.search_packs), color = c.textSecondary)
                inner()
            }
        },
    )
    for (pack in packs.filter { it.title.contains(query, true) }.sortedBy { it.isCompleted }) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (pack.premium) Modifier.border(1.dp, c.gold, RoundedCornerShape(16.dp))
                    else Modifier
                )
                .background(if (pack.premium) c.goldSurface else c.surface)
                .clickable(enabled = !busy) {
                    if (pack.locked) premiumOffer = true else openPack(pack)
                }
        ) {
            Box {
                PackCover(
                    pack.coverUrl,
                    Modifier.fillMaxWidth().aspectRatio(1.85f),
                    blurred = pack.isCompleted,
                )
                if (pack.isCompleted)
                    Spacer(Modifier.matchParentSize().background(c.surface.copy(alpha = 0.30f)))
            }
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (pack.premium)
                        AppText(
                            stringResource(
                                if (pack.locked) R.string.locked_premium else R.string.open_premium
                            ),
                            color = c.gold,
                            style = AppTheme.typography.button,
                        )
                    else Chip(stringResource(R.string.available))
                    if (pack.isCompleted) Chip(stringResource(R.string.pack_completed))
                }
                AppText(pack.title, style = AppTheme.typography.title)
                AppText(
                    pack.description,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (!busy && packs.none { it.title.contains(query, true) })
        AppText(
            if (query.isEmpty()) stringResource(R.string.no_packs)
            else stringResource(R.string.no_results)
        )
}

@Composable
internal fun PacksRoute(vm: PacksViewModel, open: (Pack) -> Unit) {
    ScreenContent(vm.request.status, stringResource(R.string.packs_title)) {
        PacksScreen(vm.state, vm.request.status.busy, vm::query, open)
    }
}
