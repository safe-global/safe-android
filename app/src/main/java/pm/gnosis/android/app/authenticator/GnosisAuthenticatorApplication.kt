package pm.gnosis.android.app.authenticator

import android.content.Context
import android.support.multidex.MultiDexApplication
import pm.gnosis.android.app.authenticator.di.component.ApplicationComponent
import pm.gnosis.android.app.authenticator.di.component.DaggerApplicationComponent
import pm.gnosis.android.app.authenticator.di.module.CoreModule
import pm.gnosis.crypto.LinuxSecureRandom
import timber.log.Timber
import timber.log.Timber.DebugTree

class GnosisAuthenticatorApplication : MultiDexApplication() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
            .coreModule(CoreModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        LinuxSecureRandom()
        //Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
    }

    companion object {
        operator fun get(context: Context): GnosisAuthenticatorApplication {
            return context.applicationContext as GnosisAuthenticatorApplication
        }
    }
}
