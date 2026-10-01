package com.paulchibamba.teleprompter.data.io

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.paulchibamba.teleprompter.data.json.BackupCodec
import com.paulchibamba.teleprompter.domain.backup.BackupSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A snapshot file sitting in the backup folder. */
data class StoredBackup(
    val uri: Uri,
    val name: String,
    val lastModified: Long,
)

/**
 * Reads and writes backup files through the Storage Access Framework.
 *
 * SAF is what makes this work at all: it needs **no permission**, which keeps the app's
 * no-permissions guarantee intact, and the folder the user picks lives outside the app sandbox so
 * an uninstall cannot take the files with it.
 *
 * One limit is inherent and the UI has to say so. The grant on that folder is stored by the system
 * *against this install*, so a reinstall loses it along with everything else. The files are still
 * there; the app just has to be pointed at them once more.
 */
class BackupStore(context: Context) {

    private val applicationContext = context.applicationContext
    private val contentResolver get() = applicationContext.contentResolver

    /**
     * Takes a long-lived grant on the folder the user picked, so it still works after a restart.
     * Without this the permission dies with the process and automatic snapshots stop silently.
     */
    fun rememberFolder(treeUri: Uri): Boolean = runCatching {
        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        true
    }.getOrDefault(false)

    /** Whether the grant on [treeUri] is still held — the user can revoke it in system settings. */
    fun canStillWriteTo(treeUri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isWritePermission } &&
            folderOf(treeUri)?.canWrite() == true

    /**
     * Writes [snapshot] into the folder as a new dated file, then trims the oldest away.
     *
     * Snapshots rotate rather than overwrite. A single file would mean a corrupt write, or a backup
     * taken after an accidental "delete all", destroys the only copy — the failure this whole step
     * exists to prevent.
     */
    suspend fun writeSnapshot(
        treeUri: Uri,
        snapshot: BackupSnapshot,
        fileName: String,
        keepAtMost: Int,
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val folder = folderOf(treeUri) ?: return@runCatching null
            val file = folder.createFile(BACKUP_MIME_TYPE, fileName) ?: return@runCatching null
            contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                out.write(BackupCodec.encode(snapshot).toByteArray())
            } ?: return@runCatching null
            trimTo(folder, keepAtMost)
            file.uri
        }.getOrNull()
    }

    /** Writes a snapshot to one exact file the user named, for "export a backup somewhere". */
    suspend fun writeSnapshotTo(fileUri: Uri, snapshot: BackupSnapshot): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openOutputStream(fileUri, "wt")?.use { out ->
                    out.write(BackupCodec.encode(snapshot).toByteArray())
                } != null
            }.getOrDefault(false)
        }

    /** @return the snapshot, or null if the file is gone, unreadable, or not a backup. */
    suspend fun readSnapshot(fileUri: Uri): BackupSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            // Newlines are preserved because the whole stream is read at once; a line loop here
            // would flatten every script in the file (docs/SPEC.md §3.1).
            val raw = contentResolver.openInputStream(fileUri)?.use { it.bufferedReader().readText() }
            raw?.let(BackupCodec::decode)
        }.getOrNull()
    }

    /** The snapshots in the folder, newest first. */
    suspend fun listSnapshots(treeUri: Uri): List<StoredBackup> = withContext(Dispatchers.IO) {
        runCatching {
            folderOf(treeUri)
                ?.listFiles()
                .orEmpty()
                .filter { it.isFile && it.name?.endsWith(BACKUP_EXTENSION) == true }
                .map { StoredBackup(it.uri, it.name.orEmpty(), it.lastModified()) }
                .sortedByDescending { it.lastModified }
        }.getOrDefault(emptyList())
    }

    private fun folderOf(treeUri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(applicationContext, treeUri)?.takeIf { it.isDirectory }

    private fun trimTo(folder: DocumentFile, keepAtMost: Int) {
        folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(BACKUP_EXTENSION) == true }
            .sortedByDescending { it.lastModified() }
            .drop(keepAtMost)
            .forEach { it.delete() }
    }

    companion object {
        const val BACKUP_EXTENSION = ".prompter.json"
        const val BACKUP_MIME_TYPE = "application/json"
    }
}
