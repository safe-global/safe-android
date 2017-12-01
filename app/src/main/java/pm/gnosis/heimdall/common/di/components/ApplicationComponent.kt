package pm.gnosis.heimdall.common.di.components

import android.app.Application
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import dagger.Component
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.di.AccountsBindingModule
import pm.gnosis.heimdall.accounts.di.AccountsModule
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.modules.*
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.di.SecurityBindingsModule
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.mnemonic.di.Bip39BindingModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AccountsBindingModule::class,
    AccountsModule::class,
    ApplicationModule::class,
    ApplicationBindingsModule::class,
    Bip39BindingModule::class,
    CoreModule::class,
    InterceptorsModule::class,
    SecurityBindingsModule::class,
    ViewModelFactoryModule::class
])
interface ApplicationComponent {
    fun application(): Application

    @ApplicationContext
    fun context(): Context

    fun accountsRepository(): AccountsRepository
    fun addressBookRepository(): AddressBookRepository
    fun safeRepository(): GnosisSafeRepository
    fun tokenRepository(): TokenRepository
    fun transactionDetailRepository(): TransactionDetailsRepository

    fun viewModelFactory(): ViewModelProvider.Factory

    fun encryptionManager(): EncryptionManager

    // Base injects
    fun inject(activity: BaseActivity)
}
