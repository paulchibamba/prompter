package com.paulchibamba.teleprompter.domain.backup

import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * Everything the app holds, in one file (docs/SPEC.md §11.2).
 *
 * This is the only copy of a user's scripts that lives outside the app sandbox, so it is the only
 * thing standing between them and an uninstall. `allowBackup="false"` (P1) means the OS keeps
 * nothing, deliberately — that privacy posture is worth having, but it makes this file the whole
 * safety net rather than a convenience.
 *
 * [version] is written so a future format change can migrate rather than refuse. Restoring a
 * snapshot from a newer version than this build understands is refused outright: silently dropping
 * fields it cannot parse would turn a backup into a lossy one at the worst possible moment.
 */
@Serializable
data class BackupSnapshot(
    /**
     * `@Required` rather than merely defaulted: every other field has a default, so without this
     * *any* JSON object decodes into an empty snapshot. Restoring one would wipe nothing and report
     * success — a file picked by mistake would look like a backup holding no scripts.
     */
    @Required val version: Int = CURRENT_VERSION,
    /** Epoch millis, so a folder of snapshots can be ordered and the newest offered first. */
    val createdAt: Long = 0L,
    val scripts: List<BackupScript> = emptyList(),
    val presets: List<BackupPreset> = emptyList(),
    val typography: TypographySettings = TypographySettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val scroll: ScrollSettings = ScrollSettings(),
) {
    val isReadable: Boolean get() = version <= CURRENT_VERSION

    val isEmpty: Boolean get() = scripts.isEmpty() && presets.none { !it.isBuiltIn }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * A script as it is written to the file. Deliberately its own type rather than reusing
 * [Script]: the domain model is free to change shape as the app grows, and a backup written last
 * year still has to open. `wordCount` is left out for the same reason — it is derived, and a
 * restore recomputes it rather than trusting a number from a file.
 */
@Serializable
data class BackupScript(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val presetId: Long? = null,
    val sortIndex: Int = 0,
)

@Serializable
data class BackupPreset(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean = false,
    val typography: TypographySettings = TypographySettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val scroll: ScrollSettings = ScrollSettings(),
)

fun Script.toBackup(): BackupScript = BackupScript(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    presetId = presetId,
    sortIndex = sortIndex,
)

/**
 * @param wordCount recomputed by the caller from [BackupScript.body], never read from the file.
 */
fun BackupScript.toScript(wordCount: Int): Script = Script(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wordCount = wordCount,
    presetId = presetId,
    sortIndex = sortIndex,
)

fun Preset.toBackup(): BackupPreset = BackupPreset(
    id = id,
    name = name,
    isBuiltIn = isBuiltIn,
    typography = typography,
    layout = layout,
    scroll = scroll,
)

fun BackupPreset.toPreset(): Preset = Preset(
    id = id,
    name = name,
    isBuiltIn = isBuiltIn,
    typography = typography,
    layout = layout,
    scroll = scroll,
)
