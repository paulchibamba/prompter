package com.paulchibamba.teleprompter.data.json

import com.paulchibamba.teleprompter.domain.backup.BackupSnapshot
import kotlinx.serialization.json.Json

/**
 * Turns a [BackupSnapshot] into the bytes that go in the file, and back.
 *
 * Unlike [SettingsCodec], **decoding here is allowed to fail**, and says so by returning null. A
 * settings blob is a cache of preferences and degrading it to defaults costs the user some tuning;
 * a backup is the only copy of their writing, and quietly "recovering" a half-parsed one would
 * restore a subset while looking like a success. Refusing lets the UI say the file is unreadable
 * and leave the other snapshots in the folder alone.
 */
object BackupCodec {

    private val json = Json {
        // A file written by a newer build carries keys this one has never seen. Skipping them beats
        // refusing the file, and the version check catches the changes that actually matter.
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Readable in a text editor, because a backup nobody can inspect is one nobody trusts.
        prettyPrint = true
    }

    fun encode(snapshot: BackupSnapshot): String = json.encodeToString(snapshot)

    /** @return the snapshot, or null if [raw] is not a backup this build can read. */
    fun decode(raw: String): BackupSnapshot? {
        val snapshot = try {
            json.decodeFromString<BackupSnapshot>(raw)
        } catch (_: IllegalArgumentException) {
            // SerializationException extends IllegalArgumentException; catching the supertype covers
            // both it and the plain form decodeFromString raises for some malformed input.
            return null
        }
        return snapshot.takeIf { it.isReadable }
    }
}
