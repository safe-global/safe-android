package pm.gnosis.heimdall.di.component

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Component
import pm.gnosis.heimdall.accounts.di.AccountsBindingModule
import pm.gnosis.heimdall.accounts.di.AccountsModule
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.module.ApplicationModule
import pm.gnosis.heimdall.di.module.CoreModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class, CoreModule::class, AccountsModule::class,
        AccountsBindingModule::class))
interface ApplicationComponent {
    fun application(): Application
    @ApplicationContext
    fun context(): Context

    fun moshi(): Moshi
    fun accountsRepository(): AccountsRepository
    fun ethereumJsonRpcRepository(): EthereumJsonRpcRepository
    fun gnosisAuthenticatorDb(): GnosisAuthenticatorDb
    fun preferencesManager(): PreferencesManager
    fun gnosisMultiSig(): GnosisMultisigWrapper
}
