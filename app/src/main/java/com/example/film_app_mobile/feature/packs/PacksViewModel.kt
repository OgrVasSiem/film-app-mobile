package com.example.film_app_mobile.feature.packs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.PacksRepository
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

@HiltViewModel
class PacksViewModel @Inject constructor(private val packsRepository: PacksRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var state by mutableStateOf(PacksState())
        private set

    init {
        viewModelScope.launch { packsRepository.catalogChanges.drop(1).collectLatest { delay(900); refresh() } }
    }

    private var pending = false
    private var forcePending = false

    fun refresh(force: Boolean = false) {
        pending = true
        forcePending = forcePending || force
        request.run {
          do {
            pending = false
            val forceNow = forcePending
            forcePending = false
            state = state.copy(packs = packsRepository.cachedPacks())
            val packs = packsRepository.packs(forceNow)
            state = state.copy(packs = packs)
          } while (pending)
        }
    }

    fun query(value: String) {
        state = state.copy(query = value)
    }
}
