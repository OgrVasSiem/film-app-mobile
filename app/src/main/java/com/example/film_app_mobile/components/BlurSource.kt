package com.example.film_app_mobile.components

import android.graphics.Bitmap
import android.os.Build
import kotlin.math.roundToInt

// On older Android versions Compose ignores Modifier.blur. Upscale a filtered thumbnail there.
internal fun Bitmap.blurSource(): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return this
    val scale = 20f / maxOf(width, height)
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
}
