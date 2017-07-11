package pm.gnosis.android.app.wallet.di.module

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.BuildConfig
import pm.gnosis.android.app.wallet.data.model.HexNumberAdapter
import pm.gnosis.android.app.wallet.data.model.WeiAdapter
import pm.gnosis.android.app.wallet.data.remote.EtherscanApi
import pm.gnosis.android.app.wallet.data.remote.InfuraApi
import pm.gnosis.android.app.wallet.di.ApplicationContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule(val application: Application) {
    companion object {
        const val SHARED_PREFS_NAME = "gnosisPrefs"

        const val INFURA_API_KEY_INTERCEPTOR = "infuraApiKeyInterceptor"
        const val INFURA_API_CLIENT = "infuraApiClient"
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

    @Provides
    @Singleton
    fun providesInfuraService(moshi: Moshi,
                              @Named(INFURA_API_CLIENT) client: OkHttpClient): InfuraApi {
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(InfuraApi.RINKEBY_BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(InfuraApi::class.java)
    }

    @Provides
    @Singleton
    @Named(INFURA_API_CLIENT)
    fun providesOkHttpClient(@Named(INFURA_API_KEY_INTERCEPTOR) interceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
    }

    @Provides
    @Singleton
    @Named(INFURA_API_KEY_INTERCEPTOR)
    fun providesApiKeyInterceptor(): Interceptor {
        return Interceptor {
            var request = it.request()
            val builder = request.url().newBuilder()
            val url = builder.addQueryParameter("token", BuildConfig.INFURA_API_KEY).build()
            request = request.newBuilder().url(url).build()
            it.proceed(request)
        }
    }
}
