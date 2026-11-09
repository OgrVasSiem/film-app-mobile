package com.example.film_app_mobile.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.core.network.model.ApiError
import com.example.film_app_mobile.core.network.model.ApiResult
import com.example.film_app_mobile.core.network.model.ResponseMetadata
import com.example.film_app_mobile.core.network.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EndpointState {
    data object Idle : EndpointState
    data object Loading : EndpointState
    data class Success(val status: String?, val metadata: ResponseMetadata) : EndpointState
    data class Failure(val error: ApiError) : EndpointState
}

data class HealthUiState(
    val server: EndpointState = EndpointState.Idle,
    val database: EndpointState = EndpointState.Idle,
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HealthUiState())
    val state = _state.asStateFlow()

    private var serverJob: Job? = null
    private var databaseJob: Job? = null

    fun checkServer() {
        if (serverJob?.isActive == true) return
        serverJob = viewModelScope.launch {
            _state.update { it.copy(server = EndpointState.Loading) }
            val next = when (val result = repository.checkServer()) {
                is ApiResult.Success -> EndpointState.Success(result.data.status, result.metadata)
                is ApiResult.Failure -> EndpointState.Failure(result.error)
            }
            _state.update { it.copy(server = next) }
        }
    }

    fun checkDatabase() {
        if (databaseJob?.isActive == true) return
        databaseJob = viewModelScope.launch {
            _state.update { it.copy(database = EndpointState.Loading) }
            val next = when (val result = repository.checkDatabase()) {
                is ApiResult.Success -> EndpointState.Success(null, result.metadata)
                is ApiResult.Failure -> EndpointState.Failure(result.error)
            }
            _state.update { it.copy(database = next) }
        }
    }
}
