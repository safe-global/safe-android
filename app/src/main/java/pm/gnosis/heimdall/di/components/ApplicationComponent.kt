package pm.gnosis.heimdall.di.components

import android.app.Application
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import dagger.Component
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationBindingsModule
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.di.modules.InterceptorsModule
import pm.gnosis.heimdall.di.modules.ViewModelFactoryModule
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.CrashTracker
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.services.HeimdallFirebaseService
import pm.gnosis.heimdall.services.HeimdallInstanceIdService
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.ticker.data.repositories.TickerRepository
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

    fun crashTracker(): CrashTracker
    fun eventTracker(): EventTracker

    fun accountsRepository(): AccountsRepository
    fun addressBookRepository(): AddressBookRepository
    fun safeRepository(): GnosisSafeRepository
    fun tickerRepository(): TickerRepository
    fun tokenRepository(): TokenRepository
    fun transactionInfoRepository(): TransactionInfoRepository

    fun viewModelFactory(): ViewModelProvider.Factory

    fun encryptionManager(): EncryptionManager
    fun qrCodeGenerator(): QrCodeGenerator
    fun addressHelper(): AddressHelper
    fun toolbarHelper(): ToolbarHelper

    // Base injects
    fun inject(activity: BaseActivity)

    fun inject(service: HeimdallFirebaseService)
    fun inject(service: HeimdallInstanceIdService)
}
