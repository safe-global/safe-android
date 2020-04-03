package io.gnosis.safe

import android.content.Context
import androidx.multidex.MultiDexApplication
import io.gnosis.safe.di.ComponentProvider
import io.gnosis.safe.di.components.ApplicationComponent
import io.gnosis.safe.di.components.DaggerApplicationComponent
import io.gnosis.safe.di.modules.ApplicationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import java.security.Security

class HeimdallApplication : MultiDexApplication(), ComponentProvider {

    private val component: ApplicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(ApplicationModule(this)).build()

    override fun get(): ApplicationComponent = component

    override fun onCreate() {
        super.onCreate()

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
