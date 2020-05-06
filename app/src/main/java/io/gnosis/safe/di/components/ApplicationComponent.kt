package io.gnosis.safe.di.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.di.modules.*
import io.gnosis.safe.helpers.AppInitManager
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.safe.terms.TermsChecker
import pm.gnosis.svalinn.common.PreferencesManager
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        InterceptorsModule::class,
        ViewModelFactoryModule::class,
        DatabaseModule::class,
        RepositoryModule::class
    ]
)
interface ApplicationComponent {
    fun application(): Application

    @ApplicationContext
    fun context(): Context


    fun appInitManager(): AppInitManager

    fun viewModelFactory(): ViewModelProvider.Factory

    fun repositories(): Repositories

    fun appDispatchers(): AppDispatchers

    fun preferencesManager(): PreferencesManager

    fun termsChecker(): TermsChecker

    // Base injects
    fun inject(activity: BaseActivity)
//
//    fun inject(service: BridgeService)
//    fun inject(service: HeimdallFirebaseService)
}
