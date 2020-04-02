package pm.gnosis.heimdall.di.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationBindingsModule
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.InterceptorsModule
import pm.gnosis.heimdall.di.modules.ViewModelFactoryModule
import pm.gnosis.heimdall.helpers.AppInitManager
import pm.gnosis.heimdall.ui.base.BaseActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        ApplicationBindingsModule::class,
        InterceptorsModule::class,
        ViewModelFactoryModule::class
    ]
)
interface ApplicationComponent {
    fun application(): Application

    @ApplicationContext
    fun context(): Context


    fun appInitManager(): AppInitManager

    fun viewModelFactory(): ViewModelProvider.Factory

    // Base injects
    fun inject(activity: BaseActivity)
//
//    fun inject(service: BridgeService)
//    fun inject(service: HeimdallFirebaseService)
}
