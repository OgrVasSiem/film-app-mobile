package com.example.film_app_mobile.feature.library

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.mapper.*
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.library.state.LibraryState
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
internal class LibraryViewModel @Inject constructor(private val repo: SectionRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(LibraryState())
    val viewState: StateFlow<LibraryState> = _viewState.asStateFlow()
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
                        section.items.take(5).map { it.entity(0).ui() }
                    },
                archiveCount = overview.sections["WONT_WATCH"]?.totalCount ?: 0L,
            )
        }
    }

    fun load(type: ContentType) =
        request.run {
            repo.cachedOverview(type)?.let(::show)
            show(repo.overview(type))
        }
}
