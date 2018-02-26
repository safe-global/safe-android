package pm.gnosis.heimdall

import android.content.Context
import android.support.multidex.MultiDexApplication
import org.mockito.Mockito
import pm.gnosis.heimdall.common.di.components.ApplicationComponent

open class HeimdallApplication : MultiDexApplication() {

    val component: ApplicationComponent = Mockito.mock(ApplicationComponent::class.java)

    companion object {
        operator fun get(context: Context): HeimdallApplication {
            return context.applicationContext as HeimdallApplication
        }
    }
}
