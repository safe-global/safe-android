package pm.gnosis.heimdall.common.di.modules

import android.arch.persistence.room.Room
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.data.adapters.HexNumberAdapter
import pm.gnosis.heimdall.data.adapters.WeiAdapter
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.remote.EthGasStationApi
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcApi
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.ticker.data.remote.TickerAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule {
    companion object {
        const val INFURA_REST_CLIENT = "infuraRestClient"
    }

    @Provides
    @Singleton
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
                .add(WeiAdapter())
                .add(HexNumberAdapter())
                .add(TickerAdapter())
                .build()
    }

    @Provides
    @Singleton
    fun providesIpfsApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient): IpfsApi {
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(IpfsApi.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(IpfsApi::class.java)
    }

    @Provides
    @Singleton
    fun providesEthereumJsonRpcApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient)
            : EthereumJsonRpcApi {
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
    fun providesEthGasStationApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient): EthGasStationApi {
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(EthGasStationApi.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(EthGasStationApi::class.java)
    }

    @Provides
    @Singleton
    fun providesPushServiceApi(moshi: Moshi, client: OkHttpClient): PushServiceApi {
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(PushServiceApi.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
        return retrofit.create(PushServiceApi::class.java)
    }

    @Provides
    @Singleton
    @Named(INFURA_REST_CLIENT)
    fun providesInfuraOkHttpClient(okHttpClient: OkHttpClient, @Named(InterceptorsModule.REST_CLIENT_INTERCEPTORS) interceptors: @JvmSuppressWildcards List<Interceptor>): OkHttpClient {
        return okHttpClient.newBuilder().apply {
            interceptors.forEach {
                addInterceptor(it)
            }
        }.build()
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(10, TimeUnit.SECONDS)
        writeTimeout(10, TimeUnit.SECONDS)
    }.build()

    @Provides
    @Singleton
    fun providesDb(@ApplicationContext context: Context) =
            Room.databaseBuilder(context, GnosisAuthenticatorDb::class.java, GnosisAuthenticatorDb.DB_NAME)
                    .build()
}
