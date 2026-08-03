package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.BuiltInPresets
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetUseCasesTest {

    @Test
    fun `saving over a built-in stores a copy and leaves the original untouched`() = runTest {
        val repository = FakePresetRepository()
        repository.ensureBuiltIns()
        val savePreset = SavePreset(repository)

        val id = savePreset(BuiltInPresets.studio.copy(typography = TypographySettings(sizeSp = 120f)))

        assertNotEquals(BuiltInPresets.STUDIO_ID, id)
        assertFalse(repository.byId(id)!!.isBuiltIn)
        assertEquals(120f, repository.byId(id)!!.typography.sizeSp, 0f)
        assertEquals(BuiltInPresets.studio, repository.byId(BuiltInPresets.STUDIO_ID))
    }

    @Test
    fun `an unnamed preset gets a fallback name rather than an empty row`() = runTest {
        val repository = FakePresetRepository()

        val id = SavePreset(repository)(Preset(name = "   "))

        assertEquals(SavePreset.UNTITLED, repository.byId(id)!!.name)
    }

    @Test
    fun `a name keeps its shape but loses its surrounding whitespace`() = runTest {
        val repository = FakePresetRepository()

        val id = SavePreset(repository)(Preset(name = "  Album commentary \n"))

        assertEquals("Album commentary", repository.byId(id)!!.name)
    }

    @Test
    fun `deleting a built-in is refused`() = runTest {
        val repository = FakePresetRepository()
        repository.ensureBuiltIns()

        assertFalse(DeletePreset(repository)(BuiltInPresets.TIGHT_GLASS_ID))
        assertNotNull(repository.byId(BuiltInPresets.TIGHT_GLASS_ID))
    }

    @Test
    fun `applying a preset writes all three blocks in one edit`() = runTest {
        val presets = FakePresetRepository()
        val settings = FakeSettingsRepository()
        presets.ensureBuiltIns()

        assertTrue(ApplyPreset(presets, settings)(BuiltInPresets.TIGHT_GLASS_ID))

        assertEquals(BuiltInPresets.tightGlass.typography, settings.typography.value)
        assertEquals(BuiltInPresets.tightGlass.layout, settings.layout.value)
        assertEquals(BuiltInPresets.tightGlass.scroll, settings.scroll.value)
        // One write, not three: a half-applied preset must never reach the prompter.
        assertEquals(1, settings.writeCount)
    }

    @Test
    fun `applying a preset that has been deleted changes nothing`() = runTest {
        val settings = FakeSettingsRepository()

        assertFalse(ApplyPreset(FakePresetRepository(), settings)(9999L))

        assertEquals(0, settings.writeCount)
    }

    @Test
    fun `the current settings can be captured as a new preset`() = runTest {
        val presets = FakePresetRepository()
        val settings = FakeSettingsRepository()
        settings.setTypography(TypographySettings(sizeSp = 64f))
        settings.setScroll(ScrollSettings(speedWpm = 180))

        val id = SaveCurrentSettingsAsPreset(settings, SavePreset(presets))("Podcast")

        val saved = presets.byId(id)!!
        assertEquals("Podcast", saved.name)
        assertFalse(saved.isBuiltIn)
        assertEquals(64f, saved.typography.sizeSp, 0f)
        assertEquals(180, saved.scroll.speedWpm)
    }

    @Test
    fun `presets are listed built-ins first`() = runTest {
        val repository = FakePresetRepository()
        SavePreset(repository)(Preset(name = "Album commentary"))

        val names = ObservePresets(repository)().first().map { it.name }

        assertEquals(BuiltInPresets.all.map { it.name } + "Album commentary", names)
    }
}
