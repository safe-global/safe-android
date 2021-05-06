package io.gnosis.safe.ui.settings.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.gnosis.data.backend.GatewayApi
import io.gnosis.safe.ui.settings.app.SettingsHandler.Companion.KEY_USER_DEFAULT_FIAT
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences

class SettingsHandlerTest {

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var gatewayApi: GatewayApi
    private val remoteConfig = mockk<FirebaseRemoteConfig>()

    @Before
    fun setup() {
        gatewayApi = mockk()
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @Test
    fun `setAllowScreenshots (true) should save true under ALLOW_SCREENSHOTS`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            settingsHandler.screenshotsAllowed = true

            assertTrue(preferences.getBoolean(SettingsHandler.KEY_ALLOW_SCREENSHOTS, false))
        }
    }

    @Test
    fun `getAllowScreenshots (initially) should return false`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.screenshotsAllowed

            assertFalse(result)
        }
    }

    @Test
    fun `getAllowScreenshots (screenshots have been authorized previously) should return true`() {
        preferences.edit().putBoolean(SettingsHandler.KEY_ALLOW_SCREENSHOTS, true)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            val result = settingsHandler.screenshotsAllowed

            assertTrue(result)
        }
    }

    @Test
    fun `nightMode (initially) should return MODE_NIGHT_FOLLOW_SYSTEM`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(result, MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_YES) should return MODE_NIGHT_YES`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_YES)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_YES, result)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_NO) should return MODE_NIGHT_NO`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_NO)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_NO, result)
        }
    }

    @Test
    fun `nightMode (for preference MODE_NIGHT_FOLLOW_SYSTEM) should return MODE_NIGHT_FOLLOW_SYSTEM`() {
        preferences.edit().putInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.nightMode

            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, result)
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_YES) should save MODE_NIGHT_YES under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_YES

            assertEquals(MODE_NIGHT_YES, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_NO))
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_NO) should save MODE_NIGHT_NO under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_NO

            assertEquals(MODE_NIGHT_NO, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM))
        }
    }

    @Test
    fun `nightMode (MODE_NIGHT_FOLLOW_SYSTEM) should save MODE_NIGHT_FOLLOW_SYSTEM under KEY_NIGHT_MODE`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            settingsHandler.nightMode = MODE_NIGHT_FOLLOW_SYSTEM

            assertEquals(MODE_NIGHT_FOLLOW_SYSTEM, preferences.getInt(SettingsHandler.KEY_NIGHT_MODE, MODE_NIGHT_YES))
        }
    }

    @Test
    fun `loadSupportedFiatCodes - backend error`() = runBlocking {
        val backendError = Throwable()
        coEvery { gatewayApi.loadSupportedCurrencies() } throws backendError
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = runCatching { settingsHandler.loadSupportedFiatCodes() }

        coVerify(exactly = 1) { gatewayApi.loadSupportedCurrencies() }
        Assert.assertEquals(backendError, actual.exceptionOrNull())
    }

    @Test
    fun `loadSupportedFiatCodes - backend sorting is used - successful`() = runBlocking {
        val supportedFiats = listOf("USD", "EUR", "CAD")
        coEvery { gatewayApi.loadSupportedCurrencies() } returns supportedFiats
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = runCatching { settingsHandler.loadSupportedFiatCodes() }

        coVerify(exactly = 1) { gatewayApi.loadSupportedCurrencies() }
        Assert.assertEquals(supportedFiats, actual.getOrNull())
    }

    @Test
    fun `getUserDefaultFiat - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.userDefaultFiat
        val expected = "USD"

        verify(exactly = 1) { preferences.getString(KEY_USER_DEFAULT_FIAT, "USD") }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `getUserDefaultFiat - value set`() = runBlocking {
        preferences.edit().putString(KEY_USER_DEFAULT_FIAT, "EUR")
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.userDefaultFiat
        val expected = "EUR"

        verify(exactly = 1) { preferences.getString(KEY_USER_DEFAULT_FIAT, "USD") }
        assertEquals(expected, actual)
    }
}
