package pm.gnosis.android.app.wallet

import android.app.Application
import android.content.Context
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerApplicationComponent
import pm.gnosis.android.app.wallet.di.module.ApplicationModule
import pm.gnosis.android.app.wallet.di.module.EthereumModule
import timber.log.Timber
import timber.log.Timber.DebugTree

class GnosisApplication : Application() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
            .ethereumModule(EthereumModule(this))
            .applicationModule(ApplicationModule(this))
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    companion object {
        operator fun get(context: Context): GnosisApplication {
            return context.applicationContext as GnosisApplication
        }
    }
}
