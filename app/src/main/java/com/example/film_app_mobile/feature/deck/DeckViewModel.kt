package com.example.film_app_mobile.feature.deck

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.LibraryRepository
import com.example.film_app_mobile.data.repository.PacksRepository
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = DeckViewModel.Factory::class)
class DeckViewModel
@dagger.assisted.AssistedInject
constructor(
    private val packsRepository: PacksRepository,
    private val libraryRepository: LibraryRepository,
    @dagger.assisted.Assisted private val pack: Pack,
) : ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var state by mutableStateOf(DeckState())
        private set

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(pack: Pack): DeckViewModel
    }

    init {
        load()
    }

    fun load() =
        request.run {
            val cards = packsRepository.films(pack.id)
            val remaining =
                if (cards.isEmpty()) packsRepository.films(pack.id, allowCache = false) else cards
            state = state.copy(cards = remaining, total = remaining.size, loaded = true)
        }

    fun choose(movie: Film, folder: String, onRejected: () -> Unit) =
        request.run {
            if (state.cards.firstOrNull()?.id != movie.id) return@run
            try {
                libraryRepository.decide(movie, folder)
            } catch (failure: Exception) {
                onRejected()
                throw failure
            }
            val remainingCards = state.cards.drop(1)
            val nextCards =
                if (folder == "WILL_WATCH" && remainingCards.size > 1) {
                    listOf(remainingCards.last()) + remainingCards.dropLast(1)
                } else remainingCards
            state =
                state.copy(
                    cards = nextCards,
                    loaded = state.cards.size > 1,
                    saved = state.saved + if (folder == "WILL_WATCH") 1 else 0,
                    skipped = state.skipped + if (folder == "WONT_WATCH") 1 else 0,
                )
            if (state.cards.isEmpty()) {
                val remaining = packsRepository.films(pack.id, allowCache = false)
                state =
                    state.copy(
                        cards = remaining,
                        loaded = true,
                        total = maxOf(state.total, remaining.size),
                    )
            }
        }
}
