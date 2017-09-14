package pm.gnosis.android.app.accounts.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.GethAccountManager
import pm.gnosis.android.app.accounts.repositories.impl.GethAccountsRepository
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import javax.inject.Singleton

@Module
class AccountsModule {
    @Provides
    @Singleton
    fun providesGethKeyStore(@ApplicationContext context: Context) = KeyStore("${context.filesDir}/keystore", Geth.LightScryptN, Geth.LightScryptP)

    @Provides
    @Singleton
    fun providesAccountRepository(accountManager: GethAccountManager, keyStore: KeyStore): AccountsRepository
            = GethAccountsRepository(accountManager, keyStore)
}