package com.example.film_app_mobile.core.storage

import android.content.Context
import androidx.room.Room
import com.example.film_app_mobile.core.storage.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FilmDatabase =
        Room.databaseBuilder(context, FilmDatabase::class.java, "films.db").build()

    @Provides fun provideProfileDao(database: FilmDatabase) = database.profile()

    @Provides fun providePackDao(database: FilmDatabase) = database.pack()

    @Provides fun provideDeckDao(database: FilmDatabase) = database.deck()

    @Provides fun provideLibraryDao(database: FilmDatabase) = database.library()
}
