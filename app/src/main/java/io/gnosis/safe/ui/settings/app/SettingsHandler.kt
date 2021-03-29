package io.gnosis.safe.ui.settings.app

import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.gnosis.data.backend.GatewayApi
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsHandler @Inject constructor(
    private val gatewayApi: GatewayApi,
    private val preferencesManager: PreferencesManager,
    private val remoteConfig: FirebaseRemoteConfig
) {

    fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                // do something here
            }
        }
    }

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

    var trackingAllowed: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_ALLOW_TRACKING, true)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_ALLOW_TRACKING, value)
            }
        }

    fun allowTracking(context: Context, allow: Boolean) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(allow)
    }

    suspend fun loadSupportedFiatCodes(): List<String> = gatewayApi.loadSupportedCurrencies()

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

    var usePasscode: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_USE_PASSCODE, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_USE_PASSCODE, value)
            }
        }

    var showPasscodeBanner: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_SHOW_PASSCODE_BANNER, true)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_SHOW_PASSCODE_BANNER, value)
            }
        }

    var useBiometrics: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_USE_BIOMETRICS, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_USE_BIOMETRICS, value)
            }
        }

    var requireToOpen: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_REQUIRE_PASSCODE_TO_OPEN_APP, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_REQUIRE_PASSCODE_TO_OPEN_APP, value)
            }
        }

    var requireForConfirmations: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS, value)
            }
        }

    companion object {
        internal const val KEY_NIGHT_MODE = "prefs.string.appearance.night_mode"
        internal const val KEY_ALLOW_SCREENSHOTS = "prefs.boolean.allow_screenshots"
        internal const val KEY_ALLOW_TRACKING = "prefs.boolean.allow_tracking"
        internal const val KEY_USER_DEFAULT_FIAT = "prefs.string.user_default_fiat"
        internal const val KEY_SHOW_OWNER_BANNER = "prefs.boolean.show_owner_banner"
        internal const val KEY_SHOW_OWNER_SCREEN = "prefs.boolean.show_owner_screen"
        internal const val KEY_APP_START_COUNT = "prefs.integer.app_start_count"
        internal const val KEY_USE_PASSCODE = "prefs.boolean.use_passcode"
        internal const val KEY_USE_BIOMETRICS = "prefs.boolean.use_biometrics"
        internal const val KEY_REQUIRE_PASSCODE_TO_OPEN_APP = "prefs.boolean.require_passcode_to_open_app"
        internal const val KEY_REQUIRE_PASSCODE_FOR_CONFIRMATIONS = "prefs.boolean.require_passcode_for_confirmations"
        internal const val KEY_SHOW_PASSCODE_BANNER = "prefs.boolean.show_passcode_banner"
    }
}
