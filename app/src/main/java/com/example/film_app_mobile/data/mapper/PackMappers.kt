package com.example.film_app_mobile.data.mapper

import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.core.storage.entity.*
import com.example.film_app_mobile.ui.model.*

internal fun PackDto.entity(owner: Long, index: Int) =
    PackEntity(owner, id, title, description, premium, locked, index, coverUrl, isCompleted)

internal fun PackEntity.ui() = Pack(id, title, description, premium, locked, coverHash, isCompleted)
