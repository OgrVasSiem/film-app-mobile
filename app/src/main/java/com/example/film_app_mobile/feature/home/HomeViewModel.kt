package com.example.film_app_mobile.feature.home

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.home.state.HomeState
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class HomeViewModel
@Inject
constructor(private val repo: SectionRepository, private val profiles: ProfileRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)

    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(HomeState())
    val viewState: StateFlow<HomeState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.premium.collect { active ->
                _viewState.update {
                    it.copy(
                        hasPremium = active,
                        showPremiumOffer = it.showPremiumOffer && active != true,
                    )
                }
            }
        }
        viewModelScope.launch {
            profiles.changes.collect { profile ->
                _viewState.update { it.copy(streak = profile?.streakDays) }
            }
        }
    }

    fun premiumOffer(visible: Boolean) {
        _viewState.update { it.copy(showPremiumOffer = visible && it.hasPremium != true) }
    }

    fun togglePeriods() {
        _viewState.update { it.copy(periodsExpanded = !it.periodsExpanded) }
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

    suspend fun preview(type: ContentType, key: String) = repo.preview(type, key)

    private var catalog: CachedCatalog?
        get() = viewState.value.catalog
        set(value) {
            _viewState.update { it.copy(catalog = value) }
        }

    private var single: FilmDto?
        get() = viewState.value.single
        set(value) {
            _viewState.update { it.copy(single = value) }
        }

    private var undo: Long?
        get() = viewState.value.undo
        set(value) {
            _viewState.update { it.copy(undo = value) }
        }

    fun load(type: ContentType) = request.run { refresh(type) }

    private suspend fun refresh(type: ContentType) {
        single = repo.cachedPick(type)?.film
        catalog = repo.cached(type)
        catalog = repo.catalog(type)
        if (catalog?.catalog?.sections?.any { it.key == "SINGLE" } == true)
            single = repo.pick(type).film
        profiles.profile()
    }

    fun choose(type: ContentType, id: Long, target: String?) =
        request.run {
            repo.decide(type, id, target)
            undo = if (target == null) null else id
            request.continueWith { refresh(type) }
        }
}
