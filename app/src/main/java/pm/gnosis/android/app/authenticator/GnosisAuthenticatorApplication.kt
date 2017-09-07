package pm.gnosis.android.app.authenticator

import android.app.Application
import android.content.Context
import pm.gnosis.android.app.accounts.di.AccountsModule
import pm.gnosis.android.app.authenticator.di.component.ApplicationComponent
import pm.gnosis.android.app.authenticator.di.component.DaggerApplicationComponent
import pm.gnosis.android.app.authenticator.di.module.ApplicationModule
import pm.gnosis.android.app.authenticator.di.module.CoreModule
import timber.log.Timber
import timber.log.Timber.DebugTree

class GnosisAuthenticatorApplication : Application() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .coreModule(CoreModule(this))
            .accountsModule(AccountsModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        //Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
    }

    companion object {
        operator fun get(context: Context): GnosisAuthenticatorApplication {
            return context.applicationContext as GnosisAuthenticatorApplication
        }
    }
}
