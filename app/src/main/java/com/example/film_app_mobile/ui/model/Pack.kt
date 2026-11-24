package com.example.film_app_mobile.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class Pack(
    val id: Long,
    val title: String,
    val description: String,
    val premium: Boolean,
    val locked: Boolean,
    val coverHash: String? = null,
    val isCompleted: Boolean = false,
)
