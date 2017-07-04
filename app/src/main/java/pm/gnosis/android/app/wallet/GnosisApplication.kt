package pm.gnosis.android.app.wallet

import android.app.Application
import android.content.Context
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerApplicationComponent
import pm.gnosis.android.app.wallet.di.module.ApplicationModule
import timber.log.Timber
import timber.log.Timber.DebugTree

class GnosisApplication : Application() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
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
