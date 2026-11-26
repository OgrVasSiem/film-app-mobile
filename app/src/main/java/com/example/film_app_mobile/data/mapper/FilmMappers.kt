package com.example.film_app_mobile.data.mapper

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.entity.*
import com.example.film_app_mobile.ui.model.*

internal fun FilmDto.entity(owner: Long) =
    FilmEntity(
        owner = owner,
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

internal fun FilmEntity.ui() =
    Film(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        year = year,
        director = director,
        actors = actors,
        durationMinutes = durationMinutes,
        genres = genres
    )
