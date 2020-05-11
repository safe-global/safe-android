package io.gnosis.safe

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class Tracker private constructor(context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics

    init {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logSafeAdd(numSafes: Int) {
        logEvent(
            Event.SAFE_ADD, mapOf(
                Param.NUM_SAFES to numSafes
            )
        )
    }

    fun logSafeRemove(numSafes: Int) {
        logEvent(
            Event.SAFE_REMOVE, mapOf(
                Param.NUM_SAFES to numSafes
            )
        )
    }

    fun logPushSettings(enabled: Boolean) {
        logEvent(
            Event.PUSH_SETTING, mapOf(
                Param.PUSH_INFO to if (enabled) ParamValues.PUSH_ENABLED else ParamValues.PUSH_DISABLED
            )
        )
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
        val SAFE_ADD = "safe_add"
        val SAFE_REMOVE = "safe_remove"
        val PUSH_SETTING = "push_setting"
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
