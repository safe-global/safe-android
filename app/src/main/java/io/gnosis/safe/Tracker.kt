package io.gnosis.safe

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Singleton

@Singleton
class Tracker private constructor(context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun setCurrentScreenId(activity: Activity, screenId: ScreenId) {
        firebaseAnalytics.setCurrentScreen(activity, screenId.value, null)
    }

    fun setNumSafes(numSafes: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_SAFES, numSafes.toString())
    }

    fun setPushInfo(enabled: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PUSH_INFO, if (enabled) ParamValues.PUSH_ENABLED else ParamValues.PUSH_DISABLED)
    }

    private fun logEvent(name: String, attrs: Map<String, Any?>?) {
        try {
            val bundle = Bundle()
            attrs?.let {
                for ((key, value) in attrs) {
                    when (value) {
                        is String -> bundle.putString(key, value)
                        is Long -> bundle.putLong(key, value)
                        is Int -> bundle.putInt(key, value)
                        is Number -> bundle.putFloat(key, value.toFloat())
                    }
                }
            }
            firebaseAnalytics.logEvent(name, bundle)
        } catch (e: Exception) {
            logException(e)
        }
    }

    private fun logException(exception: Exception) {
        FirebaseCrashlytics.getInstance().recordException(exception)
    }

    object Event {
        // Put event names here
    }

    object Param {
        val NUM_SAFES = "num_safes"
        val PUSH_INFO = "push_info"
    }

    object ParamValues {
        val PUSH_ENABLED = "enabled"
        val PUSH_DISABLED = "disabled"
    }

    companion object {

        @Volatile
        private var INSTANCE: Tracker? = null

        fun getInstance(context: Context): Tracker =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Tracker(context).also { INSTANCE = it }
            }
    }
}

enum class ScreenId(val value: String) {
    SPLASH("splash"),
    LAUNCH("launch"),
    LAUNCH_TERMS("launch_terms"),
    BALANCES_NO_SAFE("assets_no_safe"),
    BALANCES_COINS("assets_coins"),
    BALANCES_COLLECTIBLES("assets_collectibles"),
    BALANCES_COLLECTIBLES_DETAILS("assets_collectibles_details"),
    SAFE_RECEIVE("safe_receive"),
    SAFE_SELECT("safe_switch"),
    SAFE_ADD_ADDRESS("safe_add_address"),
    SAFE_ADD_NAME("safe_add_name"),
    SAFE_ADD_ENS("safe_add_ens"),
    TRANSACTIONS_NO_SAFE("transactions_no_safe"),
    TRANSACTIONS("transactions"),
    TRANSACTIONS_DETAILS("transactions_details"),
    TRANSACTIONS_DETAILS_ADVANCED("transactions_details_advanced"),
    SETTINGS_APP("settings_app"),
    SETTINGS_APP_ADVANCED("settings_app_advanced"),
    SETTINGS_APP_FIAT("settings_app_edit_fiat"),
    SETTINGS_GET_IN_TOUCH("settings_app_support"),
    SETTINGS_SAFE_NO_SAFE("settings_safe_no_safe"),
    SETTINGS_SAFE("settings_safe"),
    SETTINGS_SAFE_EDIT_NAME("settings_safe_edit_name"),
    SETTINGS_SAFE_ADVANCED("settings_safe_advanced"),
    SCANNER("camera"),
}
