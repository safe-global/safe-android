package pm.gnosis.heimdall.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.model.HexNumberAdapter
import pm.gnosis.heimdall.data.model.WeiAdapter
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcApi
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.app.core.BuildConfig
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule {
    companion object {
        const val INFURA_API_KEY_INTERCEPTOR = "infuraApiKeyInterceptor"
        const val ETHEREUM_JSON_RPC_API_CLIENT = "ethereumJsonRpcApiClient"
    }

    @Provides
    @Singleton
    fun providesMoshi() = Moshi.Builder()
            .add(WeiAdapter())
            .add(HexNumberAdapter())
            .build()

    @Provides
    @Singleton
    fun providesEthereumJsonRpcService(moshi: Moshi,
                                       @Named(ETHEREUM_JSON_RPC_API_CLIENT) client: OkHttpClient): EthereumJsonRpcApi {
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(EthereumJsonRpcApi.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(EthereumJsonRpcApi::class.java)
    }

    @Provides
    @Singleton
    @Named(ETHEREUM_JSON_RPC_API_CLIENT)
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

    @Provides
    @Singleton
    fun providesDb(@ApplicationContext context: Context) = Room.databaseBuilder(context, GnosisAuthenticatorDb::class.java, GnosisAuthenticatorDb.DB_NAME).build()
}
