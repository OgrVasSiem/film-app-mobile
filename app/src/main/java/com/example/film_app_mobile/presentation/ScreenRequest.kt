package com.example.film_app_mobile.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.film_app_mobile.R
import com.example.film_app_mobile.data.error.RequestFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class RequestStatus(val busy: Boolean = false, val error: Int? = null)

class ScreenRequest(private val scope: CoroutineScope) {
    var status by mutableStateOf(RequestStatus())
        private set

    fun clearError() {
        status = status.copy(error = null)
    }

    fun run(work: suspend () -> Unit) {
        if (status.busy) return
        status = RequestStatus(busy = true)
        scope.launch {
            try {
                work()
                status = RequestStatus()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                status =
                    RequestStatus(
                        error = (failure as? RequestFailure)?.messageId ?: R.string.request_error
                    )
            }
        }
    }
}
