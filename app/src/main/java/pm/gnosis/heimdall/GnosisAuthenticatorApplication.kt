package pm.gnosis.heimdall

import android.content.Context
import android.support.multidex.MultiDexApplication
import org.spongycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.heimdall.common.di.component.ApplicationComponent
import pm.gnosis.heimdall.common.di.component.DaggerApplicationComponent
import pm.gnosis.heimdall.common.di.module.CoreModule
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security

class GnosisAuthenticatorApplication : MultiDexApplication() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
            .coreModule(CoreModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    companion object {
        operator fun get(context: Context): GnosisAuthenticatorApplication {
            return context.applicationContext as GnosisAuthenticatorApplication
        }
    }
}
