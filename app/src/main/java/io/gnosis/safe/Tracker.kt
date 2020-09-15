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

    fun setNumSafes(numSafes: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_SAFES, numSafes.toString())
    }

    fun setPushInfo(enabled: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PUSH_INFO, if (enabled) ParamValues.PUSH_ENABLED else ParamValues.PUSH_DISABLED)
    }

    fun logScreen(screenId: ScreenId) {
        logEvent(screenId.value, null)
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
    LAUNCH("screen_launch"),
    LAUNCH_TERMS("screen_launch_terms"),
    ASSETS_NO_SAFE("screen_assets_no_safe"),
    ASSETS_COINS("screen_assets_coins"),
    ASSETS_COLLECTIBLES("screen_assets_collectibles"),
    ASSETS_COLLECTIBLES_DETAILS("screen_assets_collectibles_details"),
    SAFE_RECEIVE("screen_safe_receive"),
    SAFE_SELECT("screen_safe_switch"),
    SAFE_ADD_ADDRESS("screen_safe_add_address"),
    SAFE_ADD_NAME("screen_safe_add_name"),
    SAFE_ADD_ENS("screen_safe_add_ens"),
    TRANSACTIONS_NO_SAFE("screen_transactions_no_safe"),
    TRANSACTIONS("screen_transactions"),
    TRANSACTIONS_DETAILS("screen_transactions_details"),
    TRANSACTIONS_DETAILS_ACTION("transaction_details_action"),
    TRANSACTIONS_DETAILS_ADVANCED("screen_transactions_details_advanced"),
    SETTINGS_APP("screen_settings_app"),
    SETTINGS_APP_ADVANCED("screen_settings_app_advanced"),
    SETTINGS_APP_FIAT("screen_settings_app_edit_fiat"),
    SETTINGS_GET_IN_TOUCH("screen_settings_app_support"),
    SETTINGS_SAFE_NO_SAFE("screen_settings_safe_no_safe"),
    SETTINGS_SAFE("screen_settings_safe"),
    SETTINGS_SAFE_EDIT_NAME("screen_settings_safe_edit_name"),
    SETTINGS_SAFE_ADVANCED("screen_settings_safe_advanced"),
    SCANNER("screen_camera"),
}
