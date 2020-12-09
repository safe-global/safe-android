package io.gnosis.safe.ui.settings.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences


class SettingsHandlerTest {
    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @Test
    fun `setAllowScreenshots (true) should save true under ALLOW_SCREENSHOTS`() {
        val settingsHandler = SettingsHandler(preferencesManager)
        runBlocking {
            settingsHandler.screenshotsAllowed = true

            assertTrue(preferences.getBoolean(SettingsHandler.KEY_ALLOW_SCREENSHOTS, false))
        }
    }

    @Test
    fun `getAllowScreenshots (initially) should return false`() {
        val settingsHandler = SettingsHandler(preferencesManager)

        runBlocking {
            val result = settingsHandler.screenshotsAllowed

            assertFalse(result)
        }
    }

    @Test
    fun `getAllowScreenshots (screenshots have been authorized previously) should return true`() {
        preferences.edit().putBoolean(SettingsHandler.KEY_ALLOW_SCREENSHOTS, true)
        val settingsHandler = SettingsHandler(preferencesManager)
        runBlocking {
            val result = settingsHandler.screenshotsAllowed

            assertTrue(result)
        }
    }

    @Test
    fun `nightMode (initially) should return MODE_NIGHT_FOLLOW_SYSTEM`() {
        val settingsHandler = SettingsHandler(preferencesManager)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(result, MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_YES) should return MODE_NIGHT_YES`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_YES)
        val settingsHandler = SettingsHandler(preferencesManager)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_YES, result)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_NO) should return MODE_NIGHT_NO`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_NO)
        val settingsHandler = SettingsHandler(preferencesManager)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_NO, result)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_FOLLOW_SYSTEM) should return MODE_NIGHT_FOLLOW_SYSTEM`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM)
        val settingsHandler = SettingsHandler(preferencesManager)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, result)
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_YES) should save MODE_NIGHT_YES under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(preferencesManager)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_YES

            assertEquals(MODE_NIGHT_YES, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_NO))
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_NO) should save MODE_NIGHT_NO under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(preferencesManager)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_NO

            assertEquals(MODE_NIGHT_NO, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM))
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_FOLLOW_SYSTEM) should save MODE_NIGHT_FOLLOW_SYSTEM under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(preferencesManager)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_FOLLOW_SYSTEM

            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_YES))
        }
    }
}
