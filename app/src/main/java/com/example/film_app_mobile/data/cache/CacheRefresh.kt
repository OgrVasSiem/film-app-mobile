package com.example.film_app_mobile.data.cache

import java.io.IOException
import retrofit2.HttpException

internal suspend fun refresh(hasCache: suspend () -> Boolean, fetch: suspend () -> Unit) {
    try {
        fetch()
    } catch (e: IOException) {
        if (!hasCache()) throw e
    } catch (e: HttpException) {
        if (e.code() !in 500..599 || !hasCache()) throw e
    }
}
