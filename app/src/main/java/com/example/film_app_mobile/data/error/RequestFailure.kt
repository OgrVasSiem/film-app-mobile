package com.example.film_app_mobile.data.error

import java.io.IOException

class RequestFailure(val messageId: Int, cause: Exception? = null) : IOException(cause)
