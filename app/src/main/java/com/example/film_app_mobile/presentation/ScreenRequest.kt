package com.example.film_app_mobile.presentation

import com.example.film_app_mobile.R
import com.example.film_app_mobile.data.error.RequestFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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

class ScreenRequest(
    private val scope: CoroutineScope,
    private val onStatusChanged: (RequestStatus) -> Unit = {},
) {
    var status: RequestStatus = RequestStatus.Idle
        private set(value) {
            field = value
            onStatusChanged(value)
        }

    private var job: Job? = null
    private var retryWork: (suspend () -> Unit)? = null

    fun reset() {
        job?.cancel()
        job = null
        retryWork = null
        status = RequestStatus.Idle
    }

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
        job =
            scope.launch {
                try {
                    work()
                    ensureActive()
                    retryWork = null
                    status = RequestStatus.Success
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    ensureActive()
                    status =
                        RequestStatus.Error(
                            (failure as? RequestFailure)?.messageId ?: R.string.request_error
                        )
                }
            }
    }
}
