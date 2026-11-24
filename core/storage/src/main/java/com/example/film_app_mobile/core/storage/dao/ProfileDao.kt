package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class ProfileDao {
    @Upsert abstract suspend fun saveProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :owner")
    abstract suspend fun profile(owner: Long): ProfileEntity?

    @Query("DELETE FROM snapshots WHERE owner=:id AND scope='packs'")
    abstract suspend fun invalidateCatalog(id: Long)

    @Transaction
    open suspend fun switchAccount(profile: ProfileEntity) {
        invalidateCatalog(profile.id)
        saveProfile(profile)
    }
}
