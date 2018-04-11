package pm.gnosis.heimdall.common.di.modules

import android.arch.persistence.room.Room
import android.content.Context
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.ethereum.rpc.EthereumRpcConnector
import pm.gnosis.ethereum.rpc.RpcEthereumRepository
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcApi
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcConnector
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.adapters.HexNumberAdapter
import pm.gnosis.heimdall.data.adapters.SolidityAddressAdapter
import pm.gnosis.heimdall.data.adapters.WeiAdapter
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.TxExecutorApi
import pm.gnosis.svalinn.common.di.ApplicationContext
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
    fun providesSmartLockCredentialsClient(@ApplicationContext context: Context): CredentialsClient {
        val options = CredentialsOptions.Builder().forceEnableSaveDialog().build()
        return Credentials.getClient(context, options)
    }

    @Provides
    @Singleton
    fun providesEthereumRepository(ethereumRpcConnector: EthereumRpcConnector): EthereumRepository =
        RpcEthereumRepository(ethereumRpcConnector)

    @Provides
    @Singleton
    fun providesEthereumRpcConnector(retrofitEthereumRpcApi: RetrofitEthereumRpcApi): EthereumRpcConnector =
        RetrofitEthereumRpcConnector(retrofitEthereumRpcApi)

    @Provides
    @Singleton
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
            .add(WeiAdapter())
            .add(HexNumberAdapter())
            .add(TickerAdapter())
            .add(SolidityAddressAdapter())
            .build()
    }

    @Provides
    @Singleton
    fun providesEthereumJsonRpcApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient)
            : RetrofitEthereumRpcApi {
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.BLOCKCHAIN_NET_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        return retrofit.create(RetrofitEthereumRpcApi::class.java)
    }

    @Provides
    @Singleton
    fun providesPushServiceApi(moshi: Moshi, client: OkHttpClient): PushServiceApi {
        val retrofit = Retrofit.Builder()
            // Increase timeout since our server goes to sleeps
            .client(client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build())
            .baseUrl(PushServiceApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        return retrofit.create(PushServiceApi::class.java)
    }

    @Provides
    @Singleton
    fun providesTxExecutorApi(moshi: Moshi, client: OkHttpClient): TxExecutorApi {
        val retrofit = Retrofit.Builder()
            // Increase timeout since our server goes to sleeps
            .client(client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build())
            .baseUrl(TxExecutorApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        return retrofit.create(TxExecutorApi::class.java)
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
        Room.databaseBuilder(context, ApplicationDb::class.java, ApplicationDb.DB_NAME)
            .build()
}
