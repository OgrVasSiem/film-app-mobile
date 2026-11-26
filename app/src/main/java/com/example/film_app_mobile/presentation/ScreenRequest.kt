package com.example.film_app_mobile.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.film_app_mobile.R
import com.example.film_app_mobile.data.error.RequestFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface RequestStatus {
    data object Idle : RequestStatus

    data object Loading : RequestStatus

    data object Success : RequestStatus

    data class Error(val message: Int) : RequestStatus

    val busy
        get() = this == Loading

    val error
        get() = (this as? Error)?.message
}

class ScreenRequest(private val scope: CoroutineScope) {
    var status by mutableStateOf<RequestStatus>(RequestStatus.Idle)
        private set

    private var retryWork: (suspend () -> Unit)? = null

    fun retry() {
        retryWork?.let { run(it) }
    }

    suspend fun continueWith(work: suspend () -> Unit) {
        retryWork = work
        work()
    }

    fun clearError() {
        status = if (status is RequestStatus.Error) RequestStatus.Idle else status
    }

    fun run(work: suspend () -> Unit) {
        if (status.busy) return
        retryWork = work
        status = RequestStatus.Loading
        scope.launch {
            try {
                work()
                retryWork = null
                status = RequestStatus.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                status =
                    RequestStatus.Error(
                        (failure as? RequestFailure)?.messageId ?: R.string.request_error
                    )
            }
        }
    }
}
