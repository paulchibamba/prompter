package com.paulchibamba.teleprompter.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Exercises the repository against a real DataStore file in a temporary directory, so the
 * assertions cover the round trip through disk rather than through a stub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun createDataStore() {
        // A real scope, not a TestScope: DataStore does its own file IO on this scope, and virtual
        // time would leave those writes parked while the test waits for them.
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = DataStoreSettingsRepository(dataStore)
    }

    @After
    fun cancelScope() {
        scope.cancel()
    }

    @Test
    fun `settings start at their defaults on a fresh install`() = runTest {
        assertEquals(TypographySettings(), repository.typography.first())
        assertEquals(LayoutSettings(), repository.layout.first())
        assertEquals(ScrollSettings(), repository.scroll.first())
    }

    @Test
    fun `a written block reads back exactly`() = runTest {
        val typography = TypographySettings(sizeSp = 96f, weight = 700, lineHeightMul = 1.7f)

        repository.setTypography(typography)

        assertEquals(typography, repository.typography.first())
    }

    @Test
    fun `writing one block leaves the others alone`() = runTest {
        repository.setScroll(ScrollSettings(speedWpm = 210))

        repository.setTypography(TypographySettings(sizeSp = 40f))

        assertEquals(210, repository.scroll.first().speedWpm)
        assertEquals(40f, repository.typography.first().sizeSp, 0f)
    }

    @Test
    fun `an out-of-range value is coerced before it is stored`() = runTest {
        repository.setScroll(ScrollSettings(speedWpm = 10_000))

        assertEquals(ScrollSettings.MAX_WPM, repository.scroll.first().speedWpm)
    }

    @Test
    fun `setAll writes all three blocks`() = runTest {
        repository.setAll(
            TypographySettings(sizeSp = 56f),
            LayoutSettings(marginLeftPct = 22f),
            ScrollSettings(speedWpm = 90),
        )

        assertEquals(56f, repository.typography.first().sizeSp, 0f)
        assertEquals(22f, repository.layout.first().marginLeftPct, 0f)
        assertEquals(90, repository.scroll.first().speedWpm)
    }

    @Test
    fun `resetToDefaults forgets every block`() = runTest {
        repository.setAll(
            TypographySettings(sizeSp = 56f),
            LayoutSettings(marginLeftPct = 22f),
            ScrollSettings(speedWpm = 90),
        )

        repository.resetToDefaults()

        assertEquals(TypographySettings(), repository.typography.first())
        assertEquals(LayoutSettings(), repository.layout.first())
        assertEquals(ScrollSettings(), repository.scroll.first())
    }
}
