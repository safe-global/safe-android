package pm.gnosis.heimdall.di.modules

import android.app.Application
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
import pm.gnosis.heimdall.data.remote.DevelopmentPushServiceApi
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.TxExecutorApi
import pm.gnosis.heimdall.data.remote.impls.LocalRelayServiceApi
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator
import pm.gnosis.mnemonic.android.AndroidWordListProvider
import pm.gnosis.mnemonic.wordlists.WordListProvider
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.data.db.AccountsDatabase
import pm.gnosis.svalinn.accounts.repositories.impls.KethereumAccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.ZxingQrCodeGenerator
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.FingerprintHelper
import pm.gnosis.svalinn.security.impls.AesEncryptionManager
import pm.gnosis.svalinn.security.impls.AndroidFingerprintHelper
import pm.gnosis.ticker.data.db.TickerDatabase
import pm.gnosis.ticker.data.remote.TickerAdapter
import pm.gnosis.ticker.data.remote.TickerApi
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.impls.DefaultTickerRepository
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule(private val application: Application) {
    companion object {
        const val INFURA_REST_CLIENT = "infuraRestClient"
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
    fun providesDevelopmentPushServiceApi(moshi: Moshi, client: OkHttpClient): DevelopmentPushServiceApi {
        val retrofit = Retrofit.Builder()
            // Increase timeout since our server goes to sleeps
            .client(client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build())
            .baseUrl(DevelopmentPushServiceApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        return retrofit.create(DevelopmentPushServiceApi::class.java)
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
    fun providesRelayServiceApi(accountsRepository: AccountsRepository, ethereumRepository: EthereumRepository): RelayServiceApi {
        return LocalRelayServiceApi(accountsRepository, ethereumRepository)
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

    @Provides
    @Singleton
    fun providesAccountsRepository(
        accountsDatabase: AccountsDatabase,
        encryptionManager: EncryptionManager,
        preferencesManager: PreferencesManager
    ): AccountsRepository =
        KethereumAccountsRepository(accountsDatabase, encryptionManager, preferencesManager)

    @Provides
    @Singleton
    fun providesAccountsDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AccountsDatabase::class.java, AccountsDatabase.DB_NAME).build()

    @Provides
    @Singleton
    fun providesEncryptionManager(
        application: Application,
        preferencesManager: PreferencesManager,
        fingerprintHelper: FingerprintHelper
    ): EncryptionManager =
        AesEncryptionManager(application, preferencesManager, fingerprintHelper)

    @Provides
    @Singleton
    fun providesPreferencesManager(@ApplicationContext context: Context) = PreferencesManager(context)

    @Provides
    @Singleton
    fun providesFingerprintHelper(@ApplicationContext context: Context): FingerprintHelper = AndroidFingerprintHelper(context)

    @Provides
    fun providesBip39(wordListProvider: WordListProvider): Bip39 = Bip39Generator(wordListProvider)

    @Provides
    fun providesWordListProvider(@ApplicationContext context: Context): WordListProvider = AndroidWordListProvider(context)

    @Provides
    @Singleton
    fun providesTickerRepository(tickerApi: TickerApi, tickerDb: TickerDatabase): TickerRepository =
        DefaultTickerRepository(tickerApi, tickerDb)

    @Provides
    @Singleton
    fun providesTickerApi(moshi: Moshi, client: OkHttpClient): TickerApi {
        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(TickerApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        return retrofit.create(TickerApi::class.java)
    }

    @Provides
    @Singleton
    fun providesTickerDb(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, TickerDatabase::class.java, TickerDatabase.DB_NAME).build()

    @Provides
    @Singleton
    fun providesQrCodeGenerator(): QrCodeGenerator = ZxingQrCodeGenerator()
}
