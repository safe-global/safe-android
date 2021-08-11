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
    fun `setTrackingAllowed (true) should save true under KEY_ALLOW_TRACKING`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            settingsHandler.trackingAllowed = true

            assertTrue(preferences.getBoolean(SettingsHandler.KEY_ALLOW_TRACKING, false))
        }
    }

    @Test
    fun `getTrackingAllowed (initially) should return TRUE`() {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        runBlocking {
            val result = settingsHandler.trackingAllowed

            assertTrue(result)
        }
    }

    @Test
    fun `getTrackingAllowed (tracking have been authorized previously) should return true`() {
        preferences.edit().putBoolean(SettingsHandler.KEY_ALLOW_TRACKING, true)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)
        runBlocking {
            val result = settingsHandler.trackingAllowed

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

    @Test
    fun `setUserDefaultFiat - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.userDefaultFiat = "EUR"

        val actual = settingsHandler.userDefaultFiat
        val expected = "EUR"

        verify(exactly = 1) { preferences.putString(KEY_USER_DEFAULT_FIAT, "EUR") }
        verify(exactly = 1) { preferences.getString(key = KEY_USER_DEFAULT_FIAT, value = "USD") }
        assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerBanner - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.showOwnerBanner
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_OWNER_BANNER, true) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerBanner - value set`() = runBlocking {
        preferences.edit().putString(SettingsHandler.KEY_SHOW_OWNER_BANNER, "true")
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.showOwnerBanner
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_OWNER_BANNER, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerBanner - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.showOwnerBanner = false

        val actual = settingsHandler.showOwnerBanner
        val expected = false

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_SHOW_OWNER_BANNER, false) }
        verify(exactly = 1) { preferences.getBoolean(key = SettingsHandler.KEY_SHOW_OWNER_BANNER, value = true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerScreen - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.showOwnerScreen
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_OWNER_SCREEN, true) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerScreen - value set`() = runBlocking {
        preferences.edit().putString(SettingsHandler.KEY_SHOW_OWNER_SCREEN, "true")
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.showOwnerScreen
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_OWNER_SCREEN, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `showOwnerScreen - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.showOwnerScreen = false

        val actual = settingsHandler.showOwnerScreen
        val expected = false

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_SHOW_OWNER_SCREEN, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `appStartCount - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.appStartCount
        val expected = 0

        verify(exactly = 1) { preferences.getInt(SettingsHandler.KEY_APP_START_COUNT, 0) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `appStartCount - value set`() = runBlocking {
        preferences.edit().putInt(SettingsHandler.KEY_APP_START_COUNT, 11)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.appStartCount
        val expected = 11

        verify(exactly = 1) { preferences.getInt(SettingsHandler.KEY_APP_START_COUNT, value = 0) }
        assertEquals(expected, actual)
    }

    @Test
    fun `appStartCount - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.appStartCount = 100

        val actual = settingsHandler.appStartCount
        val expected = 100

        verify(exactly = 1) { preferences.putInt(SettingsHandler.KEY_APP_START_COUNT, 100) }
        assertEquals(expected, actual)
    }

    @Test
    fun `usePasscode - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.usePasscode
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_USE_PASSCODE, false) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `usePasscode - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_USE_PASSCODE, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.usePasscode
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_USE_PASSCODE, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `usePasscode - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.usePasscode = true

        val actual = settingsHandler.usePasscode
        val expected = true

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_USE_PASSCODE, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `showPasscodeBanner - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.showPasscodeBanner
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_PASSCODE_BANNER, value = true) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `showPasscodeBanner - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_SHOW_PASSCODE_BANNER, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.showPasscodeBanner
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_SHOW_PASSCODE_BANNER, value = true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `showPasscodeBanner - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.showPasscodeBanner = true

        val actual = settingsHandler.showPasscodeBanner
        val expected = true

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_SHOW_PASSCODE_BANNER, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `useBiometrics - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.useBiometrics
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_USE_BIOMETRICS, false) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `useBiometrics - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_USE_BIOMETRICS, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.useBiometrics
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_USE_BIOMETRICS, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `useBiometrics - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.useBiometrics = true

        val actual = settingsHandler.useBiometrics
        val expected = true

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_USE_BIOMETRICS, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToOpen - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.requirePasscodeToOpen
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_OPEN_APP, false) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToOpen - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_SHOW_PASSCODE_BANNER, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.requirePasscodeToOpen
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_OPEN_APP, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToOpen - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.requirePasscodeToOpen = true

        val actual = settingsHandler.requirePasscodeToOpen
        val expected = true

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_OPEN_APP, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeForConfirmations - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.requirePasscodeForConfirmations
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, false) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeForConfirmations - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.requirePasscodeForConfirmations
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeForConfirmations - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.requirePasscodeForConfirmations = true

        val actual = settingsHandler.requirePasscodeForConfirmations
        val expected = true

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToExportKeys - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.requirePasscodeToExportKeys
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_EXPORT_KEYS, value = true) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToExportKeys - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_EXPORT_KEYS, true)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.requirePasscodeToExportKeys
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_EXPORT_KEYS, value = true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `requirePasscodeToExportKeys - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.requirePasscodeToExportKeys = false

        val actual = settingsHandler.requirePasscodeToExportKeys
        val expected = false

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_REQUIRE_PASSCODE_TO_EXPORT_KEYS, false) }
        assertEquals(expected, actual)
    }

    @Test
    fun `askForPasscodeSetupOnFirstLaunch - no value set`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        val actual = settingsHandler.askForPasscodeSetupOnFirstLaunch
        val expected = true

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_ASK_FOR_PASSCODE_SETUP_ON_FIRST_LAUNCH, value = true) }
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `askForPasscodeSetupOnFirstLaunch - value set`() = runBlocking {
        preferences.edit().putBoolean(SettingsHandler.KEY_ASK_FOR_PASSCODE_SETUP_ON_FIRST_LAUNCH, false)
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)


        val actual = settingsHandler.askForPasscodeSetupOnFirstLaunch
        val expected = false

        verify(exactly = 1) { preferences.getBoolean(SettingsHandler.KEY_ASK_FOR_PASSCODE_SETUP_ON_FIRST_LAUNCH, value = true) }
        assertEquals(expected, actual)
    }

    @Test
    fun `askForPasscodeSetupOnFirstLaunch - set value`() = runBlocking {
        val settingsHandler = SettingsHandler(gatewayApi, preferencesManager, remoteConfig)

        settingsHandler.askForPasscodeSetupOnFirstLaunch = false

        val actual = settingsHandler.askForPasscodeSetupOnFirstLaunch
        val expected = false

        verify(exactly = 1) { preferences.putBoolean(SettingsHandler.KEY_ASK_FOR_PASSCODE_SETUP_ON_FIRST_LAUNCH, false) }
        assertEquals(expected, actual)
    }
}
