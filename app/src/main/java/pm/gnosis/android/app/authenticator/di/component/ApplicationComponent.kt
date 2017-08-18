package pm.gnosis.android.app.authenticator.di.component

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import dagger.Component
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.authenticator.data.PreferencesManager
import pm.gnosis.android.app.authenticator.data.contracts.GnosisMultisigWrapper
import pm.gnosis.android.app.authenticator.data.db.GnosisAuthenticatorDb
import pm.gnosis.android.app.authenticator.data.geth.GethRepository
import pm.gnosis.android.app.authenticator.data.remote.EthereumJsonRpcRepository
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import pm.gnosis.android.app.authenticator.di.module.ApplicationModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun application(): Application
    @ApplicationContext fun context(): Context
    fun keyStore(): KeyStore
    fun moshi(): Moshi
    fun sharedPreferences(): SharedPreferences
    fun gethRepository(): GethRepository
    fun ethereumJsonRpcRepository(): EthereumJsonRpcRepository
    fun gnosisAuthenticatorDb(): GnosisAuthenticatorDb
    fun preferencesManager(): PreferencesManager
    fun gnosisMultiSig(): GnosisMultisigWrapper
}
