package com.example.film_app_mobile.data.repository

import com.example.film_app_mobile.core.network.api.ProfileApi
import com.example.film_app_mobile.core.storage.dao.ProfileDao
import com.example.film_app_mobile.data.cache.refresh
import com.example.film_app_mobile.data.mapper.entity
import com.example.film_app_mobile.data.mapper.ui
import com.example.film_app_mobile.data.session.AccountSession
import javax.inject.Inject

class ProfileRepository
@Inject
constructor(
    private val account: AccountSession,
    private val profileApi: ProfileApi,
    private val dao: ProfileDao,
) {
    suspend fun profile() =
        account.operation {
            val id = owner()
            refresh({ dao.profile(id) != null }) {
                val profile = profileApi.profile(auth).entity()
                if (dao.profile(id)?.premium != profile.premium) { dao.invalidateCatalog(id); catalogChanged() }
                dao.saveProfile(profile)
            }
            checkNotNull(dao.profile(id)).ui()
        }
}
