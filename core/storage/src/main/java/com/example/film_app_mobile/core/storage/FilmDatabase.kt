package com.example.film_app_mobile.core.storage

import androidx.room.*
import com.example.film_app_mobile.core.storage.dao.*
import com.example.film_app_mobile.core.storage.entity.*
import org.json.JSONArray

class StringLists {
    @TypeConverter fun encode(items: List<String>): String = JSONArray(items).toString()

    @TypeConverter
    fun decode(json: String): List<String> =
        JSONArray(json).let { values -> List(values.length()) { values.getString(it) } }
}

@Database(
    entities =
        [
            ProfileEntity::class,
            PackEntity::class,
            FilmEntity::class,
            PackFilm::class,
            DecisionEntity::class,
            Snapshot::class,
        ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(StringLists::class)
abstract class FilmDatabase : RoomDatabase() {
    abstract fun profile(): ProfileDao

    abstract fun pack(): PackDao

    abstract fun deck(): DeckDao

    abstract fun library(): LibraryDao
}
