package pm.gnosis.heimdall

import android.content.Context
import android.support.multidex.MultiDexApplication
import io.reactivex.plugins.RxJavaPlugins
import org.mockito.Mockito
import org.spongycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerApplicationComponent
import pm.gnosis.heimdall.common.di.modules.CoreModule
import timber.log.Timber
import java.security.Security

open class HeimdallApplication : MultiDexApplication() {

    val component: ApplicationComponent = Mockito.mock(ApplicationComponent::class.java)

    companion object {
        operator fun get(context: Context): HeimdallApplication {
            return context.applicationContext as HeimdallApplication
        }
    }
}
