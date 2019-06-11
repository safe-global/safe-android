package pm.gnosis.heimdall

import android.content.Context
import android.os.Looper
import androidx.multidex.MultiDexApplication
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import pm.gnosis.heimdall.di.ComponentProvider
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerApplicationComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import timber.log.Timber
import java.security.Security

class HeimdallApplication : MultiDexApplication(), ComponentProvider {
    private val component: ApplicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(ApplicationModule(this)).build()

    override fun get(): ApplicationComponent = component

    override fun onCreate() {
        super.onCreate()

        // Init crash tracker to track unhandled exceptions
        component.crashTracker().init()

        RxJavaPlugins.setErrorHandler(Timber::e)

        // Enable RxAndroid's new async api
        // this prevents unnecessary handler.post calls on main thread for scheduling new messages
        // which is done by default by RxAndroid main thread scheduler
        // and thus improves frame rate by avoiding unnecessary waiting for the next frame due to VSYNC locking
        val asyncMainThreadScheduler = AndroidSchedulers.from(Looper.getMainLooper(), true)
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { asyncMainThreadScheduler }

        component.shortcutRepository().init()
        component.transactionTriggerManager().init()

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
