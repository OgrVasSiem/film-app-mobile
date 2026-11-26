package com.example.film_app_mobile.feature.folder

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.folder.state.FolderState
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class FolderViewModel @Inject constructor(private val repo: SectionRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(FolderState())
    val viewState: StateFlow<FolderState> = _viewState.asStateFlow()
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

    private var undo: Long?
        get() = viewState.value.undo
        set(value) {
            _viewState.update { it.copy(undo = value) }
        }

    fun load(type: ContentType, status: String) = request.run { refresh(type, status) }

    private suspend fun refresh(type: ContentType, status: String) {
        repo.cachedPage(type, "library/page", status)?.let {
            cards = it.items
            cursor = it.nextCursor
        }

        page(type, status, null)
    }

    fun more(type: ContentType, status: String) =
        request.run { cursor?.let { page(type, status, it) } }

    private suspend fun page(type: ContentType, status: String, after: String?) {
        val result = repo.page(type, "library/page", status, after)
        cards = result.items
        cursor = result.nextCursor
    }

    fun choose(type: ContentType, status: String, id: Long, target: String?) =
        request.run {
            repo.decide(type, id, target)
            cards = cards.filterNot { it.id == id }
            undo = if (target == null) null else id
            request.continueWith { refresh(type, status) }
        }
}
