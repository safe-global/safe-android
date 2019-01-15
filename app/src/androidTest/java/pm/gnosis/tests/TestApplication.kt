package pm.gnosis.tests

import androidx.multidex.MultiDexApplication
import androidx.test.InstrumentationRegistry
import org.mockito.Mockito
import org.spongycastle.jce.provider.BouncyCastleProvider
import pm.gnosis.crypto.LinuxSecureRandom
import pm.gnosis.heimdall.di.ComponentProvider
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerApplicationComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import timber.log.Timber
import java.security.Security

open class TestApplication : MultiDexApplication(), ComponentProvider {

    private var component: ApplicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(ApplicationModule(this)).build()

    override fun get(): ApplicationComponent = component

    override fun onCreate() {
        super.onCreate()

        try {
            LinuxSecureRandom()
        } catch (e: Exception) {
            Timber.e("Could not register LinuxSecureRandom. Using default SecureRandom.")
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    companion object Companion {
        fun mockComponent(): ApplicationComponent =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApplication).run {
                component = Mockito.mock(ApplicationComponent::class.java)
                component
            }
    }
}
