package com.example.film_app_mobile.feature.library

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.dataStore.ContentTypeDataStore
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.library.state.LibraryState
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class LibraryViewModel
@Inject
constructor(
    private val repo: SectionRepository,
    private val contentTypeStore: ContentTypeDataStore,
) : ViewModel() {

    fun selectContentType(type: ContentType) {
        viewModelScope.launch { contentTypeStore.select(type) }
    }

    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(LibraryState())
    val viewState: StateFlow<LibraryState> = _viewState.asStateFlow()
    val request =
        ScreenRequest(viewModelScope) { status -> _viewState.update { it.copy(request = status) } }

    init {
        viewModelScope.launch {
            contentTypeStore.selection.collect { type ->
                if (viewState.value.contentType != type) {
                    request.reset()
                    _viewState.update { LibraryState(contentType = type) }
                }
            }
        }
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

    private fun show(overview: Overview) {
        _viewState.update { state ->
            state.copy(
                sections =
                    overview.sections.mapValues { (_, section) ->
                        section.items.take(5).map { it.ui() }
                    },
                archiveCount = overview.sections["WONT_WATCH"]?.totalCount ?: 0L,
            )
        }
    }

    fun load(type: ContentType) {
        if (viewState.value.contentType != type) return
        request.run { show(repo.overview(type)) }
    }
}
