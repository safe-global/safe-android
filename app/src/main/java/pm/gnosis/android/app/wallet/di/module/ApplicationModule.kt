package pm.gnosis.android.app.wallet.di.module

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.di.ApplicationContext
import javax.inject.Singleton

@Module
class ApplicationModule(val application: Application) {
    @Provides
    @Singleton
    @ApplicationContext
    fun providesContext(): Context = application

    @Provides
    @Singleton
    fun providesApplication(): Application = application

    @Provides
    @Singleton
    fun providesGethKeyStore() = KeyStore("${application.filesDir}/keystore", Geth.LightScryptN, Geth.LightScryptP)
}
