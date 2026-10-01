package com.paulchibamba.teleprompter.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Where automatic snapshots go, and when the last one landed. */
data class BackupState(
    /** The folder the user chose, as a tree URI string; null means automatic backup is off. */
    val folderUri: String? = null,
    /** Epoch millis of the last successful snapshot; 0 means none yet. */
    val lastBackupAt: Long = 0L,
)

/**
 * Backup settings, kept **out** of the three settings blocks on purpose.
 *
 * Those blocks travel inside a snapshot, so putting the backup folder among them would mean
 * restoring a backup silently redirects where future backups go — most likely to a folder from
 * another phone that this install has no grant on. Automatic backup would then stop, quietly, right
 * after the one moment the user proved they needed it.
 */
class BackupPreferences(private val dataStore: DataStore<Preferences>) {

    val state: Flow<BackupState> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { preferences ->
            BackupState(
                folderUri = preferences[FOLDER_URI],
                lastBackupAt = preferences[LAST_BACKUP_AT] ?: 0L,
            )
        }

    suspend fun setFolder(treeUri: String?) {
        dataStore.edit { preferences ->
            if (treeUri == null) preferences.remove(FOLDER_URI) else preferences[FOLDER_URI] = treeUri
        }
    }

    suspend fun setLastBackupAt(epochMillis: Long) {
        dataStore.edit { preferences -> preferences[LAST_BACKUP_AT] = epochMillis }
    }

    private companion object {
        val FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val LAST_BACKUP_AT = longPreferencesKey("backup_last_at")
    }
}
