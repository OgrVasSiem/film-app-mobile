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

    private fun display(deck: com.example.film_app_mobile.data.repository.LocalDeck, preserveOrder: Boolean) {
        val byId = deck.cards.associateBy { it.id }
        val cards = if (preserveOrder) (state.cards.mapNotNull { byId[it.id] } + deck.cards).distinctBy { it.id } else deck.cards
        val seen = if (preserveOrder) state.total - state.cards.size else 0
        state = state.copy(cards = cards, total = seen + cards.size, nextCursor = deck.next, loaded = true)
    }

    fun load() = request.run {
        val cached = packsRepository.localDeck(pack.id)
        display(cached ?: packsRepository.page(pack.id), false)
        fill()
    }

    private var restarting = false

    fun restart() = request.run {
        restarting = true
        display(packsRepository.page(pack.id, restart = true), false)
        restarting = false
        fill()
    }

    fun retry() = request.run {
        if (restarting || !state.loaded) {
            display(packsRepository.page(pack.id, restart = restarting), false)
            restarting = false
        }
        fill()
    }

    private suspend fun fill() {
        while (state.cards.size <= 3) {
            val cursor = state.nextCursor ?: break
            try { display(packsRepository.page(pack.id, cursor), true) } catch (e: com.example.film_app_mobile.data.error.RequestFailure) {
                if (e.messageId == com.example.film_app_mobile.R.string.premium_required) state = DeckState()
                throw e
            }
        }
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
                    loaded = true,
                    saved = state.saved + if (folder == "WILL_WATCH") 1 else 0,
                    skipped = state.skipped + if (folder == "WONT_WATCH") 1 else 0,
                )
            fill()
        }
}
