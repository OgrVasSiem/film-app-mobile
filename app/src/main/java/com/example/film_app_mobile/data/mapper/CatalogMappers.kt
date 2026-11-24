package com.example.film_app_mobile.data.mapper

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.entity.*
import com.example.film_app_mobile.ui.model.*

internal fun ProfileDto.entity() = ProfileEntity(id, login, premium, adsEnabled)

internal fun ProfileEntity.ui() = Profile(id, login, premium, adsEnabled)

internal fun PackDto.entity(owner: Long, index: Int) =
    PackEntity(owner, id, title, description, premium, locked, index, coverUrl, isCompleted)

internal fun PackEntity.ui() = Pack(id, title, description, premium, locked, coverHash, isCompleted)

internal fun FilmDto.entity(owner: Long) =
    FilmEntity(
        owner,
        id,
        title,
        description,
        posterUrl,
        year,
        director,
        actors,
        durationMinutes,
        genres,
    )

internal fun FilmEntity.ui() =
    Film(id, title, description, posterUrl, year, director, actors, durationMinutes, genres)
