package com.example.film_app_mobile.feature.home.state

import com.example.film_app_mobile.core.models.ContentType
import com.example.film_app_mobile.core.network.dto.*
import com.example.film_app_mobile.presentation.RequestStatus

data class HomeState(
    val contentType: ContentType? = null,
    val request: RequestStatus = RequestStatus.Idle,
    val premiumStep: PremiumStep = PremiumStep.Offer,
    val purchaseLogin: String = "",
    val hasPremium: Boolean? = null,
    val showPremiumOffer: Boolean = false,
    val periodsExpanded: Boolean = false,
    val detail: FilmDto? = null,
    val streak: Int? = null,
    val previewPosters: Map<String, List<String?>> = emptyMap(),
    val catalog: CatalogDto? = null,
    val single: FilmDto? = null,
    val undo: Long? = null,
)

enum class PremiumStep {
    Offer,
    Protect,
    Checkout,
}
