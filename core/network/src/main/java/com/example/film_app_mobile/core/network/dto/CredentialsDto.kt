@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.film_app_mobile.core.network.dto

import kotlinx.serialization.Serializable

@Serializable data class CredentialsDto(val login: String, val password: String)
