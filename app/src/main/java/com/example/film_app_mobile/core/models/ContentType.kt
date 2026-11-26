package com.example.film_app_mobile.core.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.film_app_mobile.R

enum class ContentType(
    val apiKey: String,
    @StringRes val value: Int,
    @DrawableRes val icon: Int,
    @StringRes val recommendationTitle: Int,
    @StringRes val durationLabel: Int,
) {
    FILM(
        apiKey = "films",
        value = R.string.films_v2,
        icon = R.drawable.ic_film_tab,
        recommendationTitle = R.string.single_film_v2,
        durationLabel = R.string.duration,
    ),
    SERIES(
        apiKey = "series",
        value = R.string.series_v2,
        icon = R.drawable.ic_series_tab,
        recommendationTitle = R.string.single_series_v2,
        durationLabel = R.string.episode_minutes,
    ),
}
