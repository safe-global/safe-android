package io.gnosis.heimdall.di.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import io.gnosis.heimdall.di.ApplicationContext
import io.gnosis.heimdall.di.modules.ApplicationBindingsModule
import io.gnosis.heimdall.di.modules.ApplicationModule
import io.gnosis.heimdall.di.modules.InterceptorsModule
import io.gnosis.heimdall.di.modules.ViewModelFactoryModule
import io.gnosis.heimdall.helpers.AppInitManager
import io.gnosis.heimdall.ui.base.BaseActivity
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
