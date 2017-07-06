package pm.gnosis.android.app.wallet.di.module

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.di.ApplicationContext
import javax.inject.Singleton

@Module
class ApplicationModule(val application: Application) {
    companion object {
        const val SHARED_PREFS_NAME = "gnosisPrefs"
    }

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

    @Provides
    @Singleton
    fun providesMoshi() = Moshi.Builder().build()

    @Provides
    @Singleton
    fun providesSharedPreferences() = application.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
}
