package io.gnosis.safe

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.di.ComponentProvider
import io.gnosis.safe.di.components.ApplicationComponent
import io.gnosis.safe.di.components.DaggerApplicationComponent
import io.gnosis.safe.di.modules.ApplicationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security

class HeimdallApplication : MultiDexApplication(), ComponentProvider {

    private val component: ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .build()

    override fun get(): ApplicationComponent = component

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        component.appInitManager().init()

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    companion object Companion {
        operator fun get(context: Context): ApplicationComponent {
            return (context.applicationContext as ComponentProvider).get()
        }
    }
}

private class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        t?.let {
            if (priority == Log.ERROR) {
                FirebaseCrashlytics.getInstance().recordException(it)
            }
        }
    }
}
