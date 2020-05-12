package io.gnosis.safe

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Tracker @Inject constructor(@ApplicationContext context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

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
}
