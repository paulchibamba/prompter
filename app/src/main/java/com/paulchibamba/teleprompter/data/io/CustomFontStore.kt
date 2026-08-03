package com.paulchibamba.teleprompter.data.io

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Takes a font the user picked and keeps a copy the app owns.
 *
 * The spec suggests persisting the document URI and holding a read permission. This copies the
 * bytes instead, deliberately: a URI can be revoked, and the file behind it can be deleted, moved,
 * or live on an SD card that is not in the phone. A prompter that loses its typeface between takes
 * is a worse outcome than a few hundred kilobytes of storage.
 */
class CustomFontStore(context: Context) {

    private val applicationContext = context.applicationContext
    private val fontDirectory: File get() = File(applicationContext.filesDir, FONT_DIRECTORY)

    /**
     * Copies the font at [uri] into app storage.
     *
     * @return the stored file, or null if it could not be read — a picker can return a URI for
     *   something that is gone by the time we open it.
     */
    fun importFrom(uri: Uri): File? = runCatching {
        fontDirectory.mkdirs()
        val destination = File(fontDirectory, IMPORTED_FONT_NAME)

        applicationContext.contentResolver.openInputStream(uri)?.use { source ->
            destination.outputStream().use { target -> source.copyTo(target) }
        } ?: return null

        destination.takeIf { it.length() > 0 }
    }.getOrNull()

    /** The imported font, if one has been imported and is still readable. */
    fun importedFont(): File? = File(fontDirectory, IMPORTED_FONT_NAME).takeIf { it.canRead() }

    private companion object {
        const val FONT_DIRECTORY = "fonts"
        const val IMPORTED_FONT_NAME = "imported_font.ttf"
    }
}
