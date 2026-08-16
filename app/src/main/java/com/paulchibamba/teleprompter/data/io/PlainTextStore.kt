package com.paulchibamba.teleprompter.data.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.nio.charset.Charset

/** A script's text as it came out of a file. */
data class ImportedText(val suggestedTitle: String, val body: String)

/**
 * Reads and writes a single script as plain text (docs/SPEC.md §11.1, §11.2).
 *
 * **Newlines are preserved exactly.** The reference app built imported text with `append(line)` and
 * no `\n`, collapsing every script into one paragraph — the defining bug this app exists to not
 * repeat (§3.1). The whole stream is read in one call for that reason; there is no line loop here
 * and there must never be one.
 */
class PlainTextStore(context: Context) {

    private val applicationContext = context.applicationContext

    suspend fun read(uri: Uri): ImportedText? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = applicationContext.contentResolver.openInputStream(uri)
                ?.use { BufferedInputStream(it).readBytes() }
                ?: return@runCatching null

            ImportedText(
                suggestedTitle = displayNameOf(uri),
                body = bytes.toString(charsetOf(bytes)).stripByteOrderMark(),
            )
        }.getOrNull()
    }

    suspend fun write(uri: Uri, body: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)
    }

    /**
     * Picks the encoding from the file's byte order mark, falling back to UTF-8.
     *
     * A `.txt` exported from Windows is often UTF-16, and reading one as UTF-8 produces a script
     * full of null characters rather than an obvious failure — the kind of corruption that is only
     * noticed on set.
     */
    private fun charsetOf(bytes: ByteArray): Charset = when {
        bytes.startsWith(0xFF, 0xFE) -> Charsets.UTF_16LE
        bytes.startsWith(0xFE, 0xFF) -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }

    private fun displayNameOf(uri: Uri): String =
        Uri.decode(uri.lastPathSegment.orEmpty())
            .substringAfterLast('/')
            .substringAfterLast(':')
            .substringBeforeLast('.')
            .ifBlank { "Imported script" }
}

private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
    size >= prefix.size && prefix.withIndex().all { (index, byte) -> this[index] == byte.toByte() }

/**
 * A UTF-8 byte order mark decodes to a zero-width character that is invisible in the editor but
 * counts as a word and renders as a blank glyph on the prompter.
 */
private fun String.stripByteOrderMark(): String = removePrefix(BYTE_ORDER_MARK)

private const val BYTE_ORDER_MARK = "\uFEFF"
