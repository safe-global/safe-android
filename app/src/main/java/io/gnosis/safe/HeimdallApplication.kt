package io.gnosis.safe

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.BuildConfig.INTERCOM_API_KEY
import io.gnosis.safe.BuildConfig.INTERCOM_APP_ID
import io.gnosis.safe.di.ComponentProvider
import io.gnosis.safe.di.components.ApplicationComponent
import io.gnosis.safe.di.components.DaggerApplicationComponent
import io.gnosis.safe.di.modules.ApplicationModule
import io.intercom.android.sdk.Intercom
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security
import java.util.*

class HeimdallApplication : MultiDexApplication(), ComponentProvider {

    private val component: ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .build()

    override fun get(): ApplicationComponent = component

    val activityListeners = mutableListOf<AppStateListener>()
    fun registerForAppState(listener: AppStateListener) {
        activityListeners.add(listener)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks())
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        } else {
            Timber.plant(ExceptionReportingTree())
        }

        component.appInitManager().init()

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        setupIntercom()
    }

    private fun setupIntercom() {
        Intercom.initialize(this, INTERCOM_API_KEY, INTERCOM_APP_ID)
        Intercom.client().registerUnidentifiedUser()
        Intercom.client().setInAppMessageVisibility(Intercom.Visibility.GONE)
    }

    private fun activityLifecycleCallbacks() = object : ActivityLifecycleCallbacks {
        private var activeActivityCount = 0

        fun active() {
            activityListeners.forEach {
                it.appInForeground()
            }
        }

        fun inactive() {
            activityListeners.forEach {
                it.appInBackground()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceStateFoo: Bundle?) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityStarted(activity: Activity) {
            if (activeActivityCount == 0) {
                // We had no active activities, so we just became active
                active()
            }
            activeActivityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            activeActivityCount--
            if (activeActivityCount == 0) {
                // We have no more active activities, so we are inactive
                inactive()
            }
        }
    }

    companion object Companion {
        operator fun get(context: Context): ApplicationComponent {
            return (context.applicationContext as ComponentProvider).get()
        }
    }
}

interface AppStateListener {
    fun appInForeground()
    fun appInBackground()
}

private class ExceptionReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        t?.let {
            if (priority == Log.ERROR) {
                with(FirebaseCrashlytics.getInstance()) {
                    val locale = Locale.getDefault()
                    setCustomKey(CRASHLYTICS_KEY_LOCALE, "${locale.isO3Language}_${locale.isO3Country}")
                    recordException(it)
                }
            }
        }
    }

    companion object {
        private const val CRASHLYTICS_KEY_LOCALE = "locale"
    }
}

class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return "(${element.fileName}:${element.lineNumber})"
    }
}
