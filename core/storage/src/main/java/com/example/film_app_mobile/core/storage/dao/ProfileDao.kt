package com.example.film_app_mobile.core.storage.dao

import androidx.room.*
import com.example.film_app_mobile.core.storage.entity.*

@Dao
abstract class ProfileDao {
    @Query("DELETE FROM decisions WHERE owner=:id")
    protected abstract suspend fun clearDecisions(id: Long)

    @Query("DELETE FROM personal_cache WHERE owner=:id")
    protected abstract suspend fun clearPersonalCache(id: Long)

    @Query("DELETE FROM catalog_stamps WHERE owner=:id")
    protected abstract suspend fun clearCatalogStamps(id: Long)

    @Query("DELETE FROM deck_pages WHERE owner=:id")
    protected abstract suspend fun clearDeckPages(id: Long)

    @Query("DELETE FROM pack_films WHERE owner=:id")
    protected abstract suspend fun clearPackFilms(id: Long)

    @Query("DELETE FROM packs WHERE owner=:id") protected abstract suspend fun clearPacks(id: Long)

    @Query("DELETE FROM films WHERE owner=:id") protected abstract suspend fun clearFilms(id: Long)

    @Query("DELETE FROM snapshots WHERE owner=:id")
    protected abstract suspend fun clearSnapshots(id: Long)

    @Query("DELETE FROM profiles WHERE id=:id")
    protected abstract suspend fun clearProfiles(id: Long)

    @Transaction
    open suspend fun clearAccount(id: Long) {
        clearDecisions(id)
        clearPersonalCache(id)
        clearCatalogStamps(id)
        clearDeckPages(id)
        clearPackFilms(id)
        clearPacks(id)
        clearFilms(id)
        clearSnapshots(id)
        clearProfiles(id)
    }

    @Query("SELECT * FROM profiles WHERE id=:owner")
    abstract fun observe(owner: Long): kotlinx.coroutines.flow.Flow<ProfileEntity?>

    @Query(
        "DELETE FROM personal_cache WHERE owner=:id AND scope IN ('offline/films/snapshot/fetched','offline/series/snapshot/fetched','offline/films/snapshot/ready','offline/series/snapshot/ready')"
    )
    abstract suspend fun invalidateSectionSnapshots(id: Long)

    @Upsert abstract suspend fun saveProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :owner")
    abstract suspend fun profile(owner: Long): ProfileEntity?

    @Query("DELETE FROM snapshots WHERE owner=:id AND scope='packs'")
    abstract suspend fun invalidateCatalog(id: Long)

    @Transaction
    open suspend fun switchAccount(profile: ProfileEntity, newAccount: Boolean) {
        if (newAccount) clearAccount(profile.id) else invalidateCatalog(profile.id)
        saveProfile(profile)
    }
}
