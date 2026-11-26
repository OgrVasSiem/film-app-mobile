package com.example.film_app_mobile.core.models

import kotlinx.serialization.Serializable

@Serializable
enum class LibraryFolder {
    WILL_WATCH,
    WATCHED,
    WONT_WATCH,
}
