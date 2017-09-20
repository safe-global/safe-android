package pm.gnosis.heimdall.accounts.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.heimdall.di.ApplicationContext
import javax.inject.Singleton

@Module
class AccountsModule {
    @Provides
    @Singleton
    fun providesGethKeyStore(@ApplicationContext context: Context) = KeyStore("${context.filesDir}/keystore", Geth.LightScryptN, Geth.LightScryptP)
}