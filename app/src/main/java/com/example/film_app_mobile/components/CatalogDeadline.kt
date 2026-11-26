package com.example.film_app_mobile.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
internal fun CatalogDeadline(deadline: String?, busy: () -> Boolean, reload: () -> Unit) {
    LaunchedEffect(deadline) {
        if (deadline != null) {
            delay(
                (java.text
                        .SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.ROOT)
                        .parse(deadline.replace(Regex("\\.\\d+"), ""))!!
                        .time - System.currentTimeMillis())
                    .coerceAtLeast(1000)
            )
            snapshotFlow { busy() }.first { !it }
            reload()
        }
    }
}
