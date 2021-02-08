package io.gnosis.safe.ui.settings.app

import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.NightMode
import io.gnosis.data.backend.GatewayApi
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsHandler @Inject constructor(
    private val gatewayApi: GatewayApi,
    private val preferencesManager: PreferencesManager
) {
    @NightMode
    var nightMode: Int
        get() = preferencesManager.prefs.getInt(KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            preferencesManager.prefs.edit {
                putInt(KEY_NIGHT_MODE, value)
            }
        }

    fun applyNightMode(@NightMode nightMode: Int) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    var screenshotsAllowed: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_ALLOW_SCREENSHOTS, value)
            }
        }

    fun allowScreenShots(window: Window, allow: Boolean) {
        if (allow) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var userDefaultFiat: String
        get() = preferencesManager.prefs.getString(KEY_USER_DEFAULT_FIAT, "USD") ?: "USD"
        set(value) {
            preferencesManager.prefs.edit {
                putString(KEY_USER_DEFAULT_FIAT, value)
            }
        }

    var showOwnerBanner: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_SHOW_OWNER_BANNER, true)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_SHOW_OWNER_BANNER, value)
            }
        }

    var showOwnerScreen: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_SHOW_OWNER_SCREEN, true)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_SHOW_OWNER_SCREEN, value)
            }
        }

    var appStartCount: Int
        get() = preferencesManager.prefs.getInt(KEY_APP_START_COUNT, 0)
        set(value) {
            preferencesManager.prefs.edit {
                putInt(KEY_APP_START_COUNT, value)
            }
        }

    suspend fun loadSupportedFiatCodes(): List<String> = gatewayApi.loadSupportedCurrencies().sorted()

    companion object {
        internal const val KEY_NIGHT_MODE = "prefs.string.appearance.night_mode"
        internal const val KEY_ALLOW_SCREENSHOTS = "prefs.boolean.allow_screenshots"
        internal const val KEY_USER_DEFAULT_FIAT = "prefs.string.user_default_fiat"
        internal const val KEY_SHOW_OWNER_BANNER = "prefs.boolean.show_owner_banner"
        internal const val KEY_SHOW_OWNER_SCREEN = "prefs.boolean.show_owner_screen"
        internal const val KEY_APP_START_COUNT = "prefs.integer.app_start_count"
    }
}
