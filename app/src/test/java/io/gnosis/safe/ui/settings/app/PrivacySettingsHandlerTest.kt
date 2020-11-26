package io.gnosis.safe.ui.settings.app

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences


class PrivacySettingsHandlerTest {
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
        val screenshotAuthorizer = PrivacySettingsHandler(preferencesManager)
        runBlocking {
            screenshotAuthorizer.screenshotsAllowed = true

            assertTrue(preferences.getBoolean(PrivacySettingsHandler.KEY_ALLOW_SCREENSHOTS, false))
        }
    }

    @Test
    fun `getAllowScreenshots (initially) should return false`() {
        val screenshotAuthorizer = PrivacySettingsHandler(preferencesManager)

        runBlocking {
            val result = screenshotAuthorizer.screenshotsAllowed

            assertFalse(result)
        }
    }

    @Test
    fun `getAllowScreenshots (screenshots have been authorized previously) should return true`() {
        preferences.edit().putBoolean(PrivacySettingsHandler.KEY_ALLOW_SCREENSHOTS, true)
        val screenshotAuthorizer = PrivacySettingsHandler(preferencesManager)
        runBlocking {
            val result = screenshotAuthorizer.screenshotsAllowed

            assertTrue(result)
        }
    }
}
