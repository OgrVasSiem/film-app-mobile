package com.example.film_app_mobile.feature.deck

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.dataStore.ContentTypeDataStore
import com.example.film_app_mobile.core.models.*
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.deck.state.DeckState
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class DeckViewModel
@Inject
constructor(
    private val repo: SectionRepository,
    private val profiles: ProfileRepository,
    private val contentTypeStore: ContentTypeDataStore,
) : ViewModel() {

    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(DeckState())
    val viewState: StateFlow<DeckState> = _viewState.asStateFlow()
    val request =
        ScreenRequest(viewModelScope) { status -> _viewState.update { it.copy(request = status) } }

    init {
        viewModelScope.launch {
            contentTypeStore.selection.collect { type ->
                if (viewState.value.contentType != type) {
                    request.reset()
                    _viewState.update { DeckState(contentType = type) }
                }
            }
        }
    }

    fun horizontalProgress(value: Float) {
        _viewState.update { it.copy(horizontalProgress = value) }
    }

    fun downwardProgress(value: Float) {
        _viewState.update { it.copy(downwardProgress = value) }
    }

    private var detail: FilmDto?
        get() = viewState.value.detail
        set(value) {
            _viewState.update { it.copy(detail = value) }
        }

    fun dismissDetails() {
        detail = null
    }

    fun details(type: ContentType, id: Long) = request.run { detail = repo.card(type, id) }

    private var cards: List<FilmDto>
        get() = viewState.value.cards
        set(value) {
            _viewState.update { it.copy(cards = value) }
        }

    private var cursor: String?
        get() = viewState.value.cursor
        set(value) {
            _viewState.update { it.copy(cursor = value) }
        }

    private var sessionDecisions: Int
        get() = viewState.value.sessionDecisions
        set(value) {
            _viewState.update { it.copy(sessionDecisions = value) }
        }

    private var undo: Long?
        get() = viewState.value.undo
        set(value) {
            _viewState.update { it.copy(undo = value) }
        }

    private var catalog: CatalogDto?
        get() = viewState.value.catalog
        set(value) {
            _viewState.update { it.copy(catalog = value) }
        }

    private var deckState: String?
        get() = viewState.value.deckState
        set(value) {
            _viewState.update { it.copy(deckState = value) }
        }

    fun load(type: ContentType, deck: DeckSource) {
        if (viewState.value.contentType != type) return
        request.run { refresh(type, deck) }
    }

    private suspend fun refresh(type: ContentType, deck: DeckSource) {
        if (deck is DeckSource.Editorial) catalog = repo.catalog(type)
        if (deck is DeckSource.Periodic) catalog = repo.catalog(type)
        page(type, deck, null)
    }

    fun more(type: ContentType, deck: DeckSource) =
        request.run { cursor?.let { page(type, deck, it) } }

    private suspend fun page(type: ContentType, deck: DeckSource, after: String?) {
        val result =
            try {
                repo.page(type, repo.deckPath(deck), null, after, cards)
            } catch (e: com.example.film_app_mobile.data.error.RequestFailure) {
                if (e.messageId == com.example.film_app_mobile.R.string.premium_required) {
                    cards = emptyList()
                    cursor = null
                    profiles.fresh()
                    repo.catalog(type)
                }
                throw e
            }
        cards = result.items
        cursor = result.nextCursor
        deckState = result.state
    }

    fun choose(
        type: ContentType,
        deck: DeckSource,
        id: Long,
        target: String?,
        reject: () -> Unit = {},
    ) =
        request.run {
            try {
                repo.decide(type, id, target)
            } catch (e: Exception) {
                reject()
                throw e
            }
            cards = cards.filterNot { it.id == id }
            sessionDecisions =
                if (target == null) (sessionDecisions - 1).coerceAtLeast(0)
                else sessionDecisions + 1
            undo = if (target == null) null else id
            request.continueWith {
                if (target == null) refresh(type, deck)
                else {
                    catalog = repo.catalog(type)
                    if (cards.isEmpty() && cursor != null) page(type, deck, cursor)
                }
            }
        }
}
