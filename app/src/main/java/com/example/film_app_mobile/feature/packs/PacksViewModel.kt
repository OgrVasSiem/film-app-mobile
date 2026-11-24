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

@HiltViewModel
class PacksViewModel @Inject constructor(private val packsRepository: PacksRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var state by mutableStateOf(PacksState())
        private set

    init {
        viewModelScope.launch { packsRepository.catalogChanges.collect { refresh() } }
    }

    fun refresh() =
        request.run {
            val packs = packsRepository.packs()
            state = state.copy(packs = packs)
        }

    fun query(value: String) {
        state = state.copy(query = value)
    }
}
