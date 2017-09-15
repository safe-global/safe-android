package pm.gnosis.android.app.authenticator.di.component

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Component
import pm.gnosis.android.app.accounts.di.AccountsBindingModule
import pm.gnosis.android.app.accounts.di.AccountsModule
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.data.PreferencesManager
import pm.gnosis.android.app.authenticator.data.contracts.GnosisMultisigWrapper
import pm.gnosis.android.app.authenticator.data.db.GnosisAuthenticatorDb
import pm.gnosis.android.app.authenticator.data.remote.EthereumJsonRpcRepository
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import pm.gnosis.android.app.authenticator.di.module.ApplicationModule
import pm.gnosis.android.app.authenticator.di.module.CoreModule
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
