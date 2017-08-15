package pm.gnosis.android.app.authenticator

import android.app.Application
import android.content.Context
import pm.gnosis.android.app.authenticator.di.component.ApplicationComponent
import pm.gnosis.android.app.authenticator.di.component.DaggerApplicationComponent
import pm.gnosis.android.app.authenticator.di.module.ApplicationModule
import timber.log.Timber
import timber.log.Timber.DebugTree

class GnosisAuthenticatorApplication : Application() {
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
        operator fun get(context: Context): GnosisAuthenticatorApplication {
            return context.applicationContext as GnosisAuthenticatorApplication
        }
    }
}
