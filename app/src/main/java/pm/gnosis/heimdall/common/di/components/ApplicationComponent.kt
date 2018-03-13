package pm.gnosis.heimdall.common.di.components

import android.app.Application
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import dagger.Component
import pm.gnosis.heimdall.common.di.modules.ApplicationBindingsModule
import pm.gnosis.heimdall.common.di.modules.ApplicationModule
import pm.gnosis.heimdall.common.di.modules.InterceptorsModule
import pm.gnosis.heimdall.common.di.modules.ViewModelFactoryModule
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.reporting.CrashTracker
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.services.HeimdallFirebaseService
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.mnemonic.di.Bip39BindingModule
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.di.AccountsBindingModule
import pm.gnosis.svalinn.accounts.di.AccountsModule
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.di.modules.CoreModule
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.di.SecurityBindingsModule
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.di.TickerBindingModule
import pm.gnosis.ticker.di.TickerModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AccountsBindingModule::class,
        AccountsModule::class,
        ApplicationModule::class,
        ApplicationBindingsModule::class,
        Bip39BindingModule::class,
        CoreModule::class,
        InterceptorsModule::class,
        TickerBindingModule::class,
        TickerModule::class,
        SecurityBindingsModule::class,
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
    fun billingRepository(): BillingRepository
    fun safeRepository(): GnosisSafeRepository
    fun signaturePushRepositoryRepository(): SignaturePushRepository
    fun tickerRepository(): TickerRepository
    fun tokenRepository(): TokenRepository
    fun transactionDetailRepository(): TransactionDetailsRepository

    fun viewModelFactory(): ViewModelProvider.Factory

    fun encryptionManager(): EncryptionManager
    fun qrCodeGenerator(): QrCodeGenerator

    // Base injects
    fun inject(activity: BaseActivity)

    fun inject(service: HeimdallFirebaseService)
}
