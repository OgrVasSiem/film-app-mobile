package com.example.film_app_mobile.data.mapper

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.ui.model.*

internal fun FilmDto.ui() =
    Film(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        year = year,
        director = director,
        actors = actors,
        durationMinutes = durationMinutes,
        genres = genres,
    )
