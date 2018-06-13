package pm.gnosis.heimdall

import android.content.Context
import android.support.multidex.MultiDexApplication
import io.reactivex.plugins.RxJavaPlugins
import org.spongycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerApplicationComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import timber.log.Timber
import java.security.Security

class HeimdallApplication : MultiDexApplication() {
    val component: ApplicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(ApplicationModule(this)).build()

    override fun onCreate() {
        super.onCreate()

        // Init crash tracker to track unhandled exceptions
        component.crashTracker().init()
        RxJavaPlugins.setErrorHandler(Timber::e)

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    companion object {
        operator fun get(context: Context): HeimdallApplication {
            return context.applicationContext as HeimdallApplication
        }
    }
}
