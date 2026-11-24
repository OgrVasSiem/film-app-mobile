package com.example.film_app_mobile.feature.profile

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.data.repository.AuthRepository
import com.example.film_app_mobile.data.repository.ProfileRepository
import com.example.film_app_mobile.presentation.ScreenRequest
import com.example.film_app_mobile.ui.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
@Inject
constructor(private val profiles: ProfileRepository, private val auth: AuthRepository) :
    ViewModel() {
    val request = ScreenRequest(viewModelScope)
    var profile by mutableStateOf<Profile?>(null)
        private set

    init {
        load()
    }

    fun load() = request.run { profile = profiles.profile() }

    fun logout() = request.run { auth.logout() }
}
