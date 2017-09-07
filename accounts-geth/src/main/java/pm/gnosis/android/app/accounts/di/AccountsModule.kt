package pm.gnosis.android.app.accounts.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.GethAccountManager
import pm.gnosis.android.app.accounts.repositories.impl.GethAccountsRepository
import javax.inject.Singleton

@Module
class AccountsModule(val context: Context) {
    @Provides
    @Singleton
    fun providesGethKeyStore() = KeyStore("${context.filesDir}/keystore", Geth.LightScryptN, Geth.LightScryptP)

    @Provides
    @Singleton
    fun providesAccountRepository(accountManager: GethAccountManager, keyStore: KeyStore): AccountsRepository
            = GethAccountsRepository(accountManager, keyStore)
}