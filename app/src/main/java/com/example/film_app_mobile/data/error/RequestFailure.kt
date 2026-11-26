package com.example.film_app_mobile.data.error

import java.io.IOException

class RequestFailure(val messageId: Int, cause: Exception? = null) : IOException(cause)

internal val RequestFailure.temporary: Boolean
    get() =
        when (val reason = cause) {
            is retrofit2.HttpException ->
                reason.code() in 500..599 || reason.code() in listOf(408, 429)
            is IOException -> true
            else -> false
        }
