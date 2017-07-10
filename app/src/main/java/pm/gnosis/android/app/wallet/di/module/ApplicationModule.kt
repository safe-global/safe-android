package pm.gnosis.android.app.wallet.di.module

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.data.model.HexNumberAdapter
import pm.gnosis.android.app.wallet.data.model.WeiAdapter
import pm.gnosis.android.app.wallet.data.remote.EtherscanApi
import pm.gnosis.android.app.wallet.di.ApplicationContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
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
    fun providesMoshi() = Moshi.Builder()
            .add(WeiAdapter())
            .add(HexNumberAdapter())
            .build()

    @Provides
    @Singleton
    fun providesSharedPreferences() = application.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesEtherscanService(moshi: Moshi): EtherscanApi {
        val retrofit = Retrofit.Builder()
                .baseUrl(EtherscanApi.RINKEBY_BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(EtherscanApi::class.java)
    }
}
