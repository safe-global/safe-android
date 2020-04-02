package io.gnosis.tests

import androidx.multidex.MultiDexApplication
import androidx.test.platform.app.InstrumentationRegistry
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.mockito.Mockito
import pm.gnosis.crypto.LinuxSecureRandom
import io.gnosis.heimdall.di.ComponentProvider
import io.gnosis.heimdall.di.components.ApplicationComponent
import io.gnosis.heimdall.di.components.DaggerApplicationComponent
import io.gnosis.heimdall.di.modules.ApplicationModule
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
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as _root_ide_package_.io.gnosis.tests.TestApplication).run {
                component = Mockito.mock(ApplicationComponent::class.java)
                component
            }
    }
}
