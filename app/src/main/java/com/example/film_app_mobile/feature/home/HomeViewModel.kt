package com.example.film_app_mobile.feature.home

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.film_app_mobile.core.dataStore.ContentTypeDataStore
import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.data.repository.SectionRepository
import com.example.film_app_mobile.feature.home.state.HomeState
import com.example.film_app_mobile.feature.home.state.PremiumStep
import com.example.film_app_mobile.presentation.ScreenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
internal class HomeViewModel
@Inject
constructor(
    private val repo: SectionRepository,
    private val profiles: ProfileRepository,
    private val contentTypeStore: ContentTypeDataStore,
) : ViewModel() {

    fun selectContentType(type: ContentType) {
        viewModelScope.launch { contentTypeStore.select(type) }
    }

    val changes
        get() = repo.changes

    private val _viewState = MutableStateFlow(HomeState())
    val viewState: StateFlow<HomeState> = _viewState.asStateFlow()

    val request =
        ScreenRequest(viewModelScope) { status -> _viewState.update { it.copy(request = status) } }

    init {
        viewModelScope.launch {
            contentTypeStore.selection.collect { type ->
                if (viewState.value.contentType != type) {
                    request.reset()
                    _viewState.update {
                        HomeState(
                            contentType = type,
                            hasPremium = it.hasPremium,
                            streak = it.streak,
                        )
                    }
                }
            }
        }
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
        _viewState.update {
            it.copy(
                showPremiumOffer = visible && it.hasPremium != true,
                premiumStep = PremiumStep.Offer,
                purchaseLogin = "",
            )
        }
    }

    fun checkPurchase() =
        request.run {
            val profile = profiles.fresh()
            _viewState.update {
                it.copy(
                    purchaseLogin = profile.login,
                    premiumStep =
                        if (profile.passwordProtected) PremiumStep.Checkout else PremiumStep.Protect,
                )
            }
        }

    fun protectForPurchase(password: String) =
        request.run {
            if (profiles.protect(password).passwordProtected) {
                _viewState.update { it.copy(premiumStep = PremiumStep.Checkout) }
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

    private var catalog: CatalogDto?
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

    fun load(type: ContentType) {
        if (viewState.value.contentType != type) return
        request.run { refresh(type) }
    }

    private suspend fun refresh(type: ContentType) {
        catalog = repo.catalog(type)
        single =
            if (catalog?.sections?.any { it.key == "SINGLE" } == true) repo.pick(type).film
            else null
        profiles.fresh()
        loadPreviewPosters(type)
    }

    private suspend fun loadPreviewPosters(type: ContentType) {
        val sections = catalog?.sections.orEmpty()
        val previews =
            sections
                .filter {
                    it.state == "READY" && it.key in setOf("DAY", "WEEK", "MONTH", "FOR_YOU")
                }
                .associate { section ->
                    val posters =
                        try {
                            repo.preview(type, section.key)
                        } catch (_: java.io.IOException) {
                            viewState.value.previewPosters[section.key].orEmpty()
                        }
                    section.key to posters
                }
        _viewState.update { it.copy(previewPosters = previews) }
    }

    fun choose(type: ContentType, id: Long, target: String?) =
        request.run {
            repo.decide(type, id, target)
            undo = if (target == null) null else id
            request.continueWith { refresh(type) }
        }
}
