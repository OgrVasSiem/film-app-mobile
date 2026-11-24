package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class ProfileDao {
    @Upsert abstract suspend fun saveProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :owner")
    abstract suspend fun profile(owner: Long): ProfileEntity?

    @Query("DELETE FROM packs WHERE owner != :owner")
    protected abstract suspend fun clean_packs(owner: Long)

    @Query("DELETE FROM films WHERE owner != :owner")
    protected abstract suspend fun clean_films(owner: Long)

    @Query("DELETE FROM pack_films WHERE owner != :owner")
    protected abstract suspend fun clean_pack_films(owner: Long)

    @Query("DELETE FROM profiles WHERE id != :owner")
    protected abstract suspend fun clean_profiles(owner: Long)

    @Query("DELETE FROM snapshots WHERE owner != :owner")
    protected abstract suspend fun clean_snapshots(owner: Long)

    @Query("DELETE FROM decisions WHERE owner != :owner AND NOT pending")
    protected abstract suspend fun clean_decisions(owner: Long)

    @Transaction
    open suspend fun switchAccount(profile: ProfileEntity) {
        clean_packs(profile.id)
        clean_films(profile.id)
        clean_pack_films(profile.id)
        clean_profiles(profile.id)
        clean_snapshots(profile.id)
        clean_decisions(profile.id)
        saveProfile(profile)
    }
}
