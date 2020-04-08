package io.gnosis.safe.di.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import dagger.Component
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.di.modules.ApplicationBindingsModule
import io.gnosis.safe.di.modules.ApplicationModule
import io.gnosis.safe.di.modules.InterceptorsModule
import io.gnosis.safe.di.modules.ViewModelFactoryModule
import io.gnosis.safe.helpers.AppInitManager
import io.gnosis.safe.ui.base.BaseActivity
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
