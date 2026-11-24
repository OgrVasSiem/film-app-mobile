package com.example.film_app_mobile.feature.packs

import com.example.film_app_mobile.ui.model.Pack

data class PacksState(val packs: List<Pack> = emptyList(), val query: String = "")
