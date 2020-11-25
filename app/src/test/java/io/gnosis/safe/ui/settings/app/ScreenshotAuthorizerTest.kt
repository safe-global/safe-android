package io.gnosis.safe.ui.settings.app

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.Assert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences


class ScreenshotAuthorizerTest {
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
        val screenshotAuthorizer = ScreenshotAuthorizer(preferencesManager)
        runBlocking {
            screenshotAuthorizer.setAllowScreenshots(true)

            Assert.assertTrue(preferences.getBoolean(ScreenshotAuthorizer.ALLOW_SCREENSHOTS, false))
        }
    }

    @Test
    fun `getAllowScreenshots (initially) should return false`() {
        val screenshotAuthorizer = ScreenshotAuthorizer(preferencesManager)

        runBlocking {
            val result = screenshotAuthorizer.getAllowScreenshots()

            Assert.assertFalse(result)
        }
    }

    @Test
    fun `getAllowScreenshots (screenshots have been authorized previously) should return true`() {
        preferences.edit().putBoolean(ScreenshotAuthorizer.ALLOW_SCREENSHOTS, true)
        val screenshotAuthorizer = ScreenshotAuthorizer(preferencesManager)
        runBlocking {
            val result = screenshotAuthorizer.getAllowScreenshots()

            Assert.assertTrue(result)
        }
    }
}
