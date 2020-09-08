package io.gnosis.safe.di.components

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.di.modules.*
import io.gnosis.safe.helpers.AppInitManager
import io.gnosis.safe.notifications.NotificationManager
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.notifications.firebase.HeimdallFirebaseService
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.activity.BaseActivity
import io.gnosis.safe.ui.base.fragment.BaseDialogFragment
import io.gnosis.safe.ui.base.fragment.BaseFragment
import io.gnosis.safe.ui.terms.TermsChecker
import io.gnosis.safe.utils.BalanceFormatter
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

    fun appDispatchers(): AppDispatchers

    fun connectivityManager(): ConnectivityManager

    fun balanceFormatter(): BalanceFormatter

    fun termsChecker(): TermsChecker

    fun tracker(): Tracker

    fun notificationManager(): NotificationManager

    fun notificationRepo(): NotificationRepository

    //TODO: remove
    fun safeRepository(): SafeRepository

    // Base injects
    fun inject(activity: BaseActivity)

    fun inject(fragment: BaseFragment)

    fun inject(fragment: BaseDialogFragment)

//    fun inject(service: BridgeService)
    fun inject(service: HeimdallFirebaseService)
}
