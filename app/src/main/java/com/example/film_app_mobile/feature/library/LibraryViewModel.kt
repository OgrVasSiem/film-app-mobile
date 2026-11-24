package com.example.film_app_mobile.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.LibraryRepository
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(private val libraryRepository: LibraryRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var state by mutableStateOf(LibraryState())
        private set

    init {
        load("WILL_WATCH")
    }

    fun load(folder: String) =
        request.run {
            val movies = libraryRepository.library(folder)
            state = LibraryState(movies, folder)
        }

    fun move(movie: Film?) {
        state = state.copy(moving = movie)
    }

    fun reset(movie: Film) =
        request.run {
            libraryRepository.reset(movie.id)
            state = state.copy(cards = state.cards.filterNot { it.id == movie.id }, moving = null)
        }

    fun choose(movie: Film, folder: String) =
        request.run {
            libraryRepository.decide(movie, folder)
            state =
                state.copy(
                    cards = state.cards.filterNot { item -> item.id == movie.id },
                    moving = null,
                )
        }
}
