package io.gnosis.safe

import android.content.Context
import androidx.multidex.MultiDexApplication
import io.gnosis.safe.di.components.ApplicationComponent
import org.mockito.Mockito

open class HeimdallApplication : MultiDexApplication() {

    val component: ApplicationComponent = Mockito.mock(ApplicationComponent::class.java)

    companion object {
        operator fun get(context: Context): HeimdallApplication {
            return context.applicationContext as HeimdallApplication
        }
    }
}
