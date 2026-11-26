package com.example.film_app_mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ClickerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            databaseList().filter { legacyDatabaseName.matches(it) }.forEach { deleteDatabase(it) }
            File(filesDir, "covers").deleteRecursively()
            File(cacheDir, "posters").deleteRecursively()
            File(noBackupFilesDir, "posters").deleteRecursively()
            cacheDir
                .listFiles()
                ?.filter {
                    legacyDatabaseName.matches(it.name.removeSuffix(".lck")) &&
                        it.name.endsWith(".lck")
                }
                ?.forEach { it.delete() }
            for (suffix in listOf("", "-wal", "-shm", "-journal")) {
                File(noBackupFilesDir, "androidx.work.workdb" + suffix).delete()
                File(getDatabasePath("androidx.work.workdb").path + suffix).delete()
            }
        }
    }

    private companion object {
        val legacyDatabaseName = Regex("films(?:-[a-f0-9]{64})?\\.db")
    }
}
