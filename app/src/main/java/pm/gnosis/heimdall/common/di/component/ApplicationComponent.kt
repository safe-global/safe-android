package pm.gnosis.heimdall.common.di.component

import android.app.Application
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Component
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.di.AccountsBindingModule
import pm.gnosis.heimdall.accounts.di.AccountsModule
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.module.ApplicationBindingsModule
import pm.gnosis.heimdall.common.di.module.ApplicationModule
import pm.gnosis.heimdall.common.di.module.CoreModule
import pm.gnosis.heimdall.common.di.module.ViewModelFactoryModule
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.di.SecurityBindingsModule
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.mnemonic.di.Bip39BindingModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AccountsBindingModule::class,
        AccountsModule::class,
        ApplicationModule::class,
        ApplicationBindingsModule::class,
        Bip39BindingModule::class,
        CoreModule::class,
        SecurityBindingsModule::class,
        ViewModelFactoryModule::class
))
interface ApplicationComponent {
    fun application(): Application

    @ApplicationContext
    fun context(): Context

    fun moshi(): Moshi

    fun accountsRepository(): AccountsRepository
    fun ethereumJsonRpcRepository(): EthereumJsonRpcRepository
    fun multisigRepository(): MultisigRepository
    fun tokenRepository(): TokenRepository

    fun viewModelFactory(): ViewModelProvider.Factory

    fun preferencesManager(): PreferencesManager
    fun encryptionManager(): EncryptionManager

    fun gnosisAuthenticatorDb(): GnosisAuthenticatorDb

    fun gnosisMultiSig(): GnosisMultisigWrapper

    // Base injects
    fun inject(activity: BaseActivity)
}
