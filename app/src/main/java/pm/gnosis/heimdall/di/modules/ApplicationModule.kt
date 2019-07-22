package pm.gnosis.heimdall.di.modules

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.walletconnect.Session
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSessionStore
import pm.gnosis.eip712.EIP712JsonParser
import pm.gnosis.eip712.adapters.moshi.MoshiAdapter
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.ethereum.rpc.EthereumRpcConnector
import pm.gnosis.ethereum.rpc.RpcEthereumRepository
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcApi
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcConnector
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.adapters.*
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.data.preferences.PreferencesToken
import pm.gnosis.heimdall.data.preferences.PreferencesWalletConnect
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.repositories.impls.RpcProxyApi
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39Generator
import pm.gnosis.mnemonic.android.AndroidWordListProvider
import pm.gnosis.mnemonic.wordlists.WordListProvider
import pm.gnosis.svalinn.accounts.data.db.AccountsDatabase
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.ZxingQrCodeGenerator
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.FingerprintHelper
import pm.gnosis.svalinn.security.KeyStorage
import pm.gnosis.svalinn.security.impls.AesEncryptionManager
import pm.gnosis.svalinn.security.impls.AndroidFingerprintHelper
import pm.gnosis.svalinn.security.impls.AndroidKeyStorage
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule(private val application: Application) {
    companion object {
        const val INFURA_REST_CLIENT = "infuraRestClient"
    }

    data class AppCoroutineDispatchers(
        val background: CoroutineDispatcher,
        val database: CoroutineDispatcher,
        val disk: CoroutineDispatcher,
        val network: CoroutineDispatcher,
        val main: CoroutineDispatcher
    )

    data class AppRxSchedulers(
        val database: Scheduler,
        val disk: Scheduler,
        val network: Scheduler,
        val main: Scheduler
    )

    @Singleton
    @Provides
    fun provideDispatchers(schedulers: AppRxSchedulers) =
        AppCoroutineDispatchers(
            background = Dispatchers.Default,
            database = schedulers.database.asCoroutineDispatcher(),
            disk = schedulers.disk.asCoroutineDispatcher(),
            network = schedulers.network.asCoroutineDispatcher(),
            main = Dispatchers.Main
        )

    @Singleton
    @Provides
    fun provideRxSchedulers() = AppRxSchedulers(
        database = Schedulers.single(),
        disk = Schedulers.io(),
        network = Schedulers.io(),
        main = AndroidSchedulers.mainThread()
    )

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
            .add(DecimalNumberAdapter())
            .add(DefaultNumberAdapter())
            .add(SolidityAddressAdapter())
            .build()
    }

    @Provides
    @Singleton
    fun providesRpcProxyApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient): RpcProxyApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.BLOCKCHAIN_NET_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
            .create(RpcProxyApi::class.java)

    @Provides
    @Singleton
    fun providesEthereumJsonRpcApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient): RetrofitEthereumRpcApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.BLOCKCHAIN_NET_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
            .create(RetrofitEthereumRpcApi::class.java)

    @Provides
    @Singleton
    fun providesPushServiceApi(moshi: Moshi, client: OkHttpClient): PushServiceApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.NOTIFICATION_SERVICE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
            .create(PushServiceApi::class.java)

    @Provides
    @Singleton
    fun providesRelayServiceApi(moshi: Moshi, client: OkHttpClient): RelayServiceApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.RELAY_SERVICE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
            .create(RelayServiceApi::class.java)

    @Provides
    @Singleton
    @Named(INFURA_REST_CLIENT)
    fun providesInfuraOkHttpClient(okHttpClient: OkHttpClient, @Named(InterceptorsModule.REST_CLIENT_INTERCEPTORS) interceptors: @JvmSuppressWildcards List<Interceptor>): OkHttpClient =
        okHttpClient.newBuilder().apply {
            interceptors.forEach {
                addInterceptor(it)
            }
        }.build()

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(10, TimeUnit.SECONDS)
        writeTimeout(10, TimeUnit.SECONDS)
        pingInterval(5, TimeUnit.SECONDS)
        certificatePinner(
            CertificatePinner.Builder().apply {
                BuildConfig.PINNED_URLS.split(",").forEach { pinnedUrl ->
                    BuildConfig.PINNED_ROOT_CERTIFICATE_HASHES.split(",").forEach { hash ->
                        add(pinnedUrl, hash)
                    }
                }
            }.build()
        )
    }.build()

    @Provides
    @Singleton
    fun providesSessionStore(@ApplicationContext context: Context, moshi: Moshi): WCSessionStore =
        FileWCSessionStore(File(context.cacheDir, "session_store.json").apply { createNewFile() }, moshi) // TODO implement proper store

    @Provides
    @Singleton
    fun providesSessionPayloadAdapter(moshi: Moshi): Session.PayloadAdapter = MoshiPayloadAdapter(moshi)

    @Provides
    @Singleton
    fun providesSessionTransportBuilder(client: OkHttpClient, moshi: Moshi): Session.Transport.Builder = OkHttpTransport.Builder(client, moshi)

    @Provides
    @Singleton
    fun providesDb(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ApplicationDb::class.java, ApplicationDb.DB_NAME)
            .addMigrations(ApplicationDb.MIGRATION_1_2)
            .addMigrations(ApplicationDb.MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun providesAccountsDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AccountsDatabase::class.java, AccountsDatabase.DB_NAME).build()

    @Provides
    @Singleton
    fun providesKeyStorage(@ApplicationContext context: Context): KeyStorage =
        AndroidKeyStorage(context)

    @Provides
    @Singleton
    fun providesEncryptionManager(
        application: Application,
        preferencesManager: PreferencesManager,
        fingerprintHelper: FingerprintHelper,
        keyStorage: KeyStorage
    ): EncryptionManager =
        // We use 4k iterations to keep the memory used during password setup below 16mb (theoretical minimum vm heap for Android 4.4)
        AesEncryptionManager(application, preferencesManager, fingerprintHelper, keyStorage, 4096)

    @Provides
    @Singleton
    fun providesPreferencesManager(@ApplicationContext context: Context) = PreferencesManager(context)

    @Provides
    @Singleton
    fun providesWalletConnectPreferences(@ApplicationContext context: Context) = PreferencesWalletConnect(context)

    @Provides
    @Singleton
    fun providesSafePreferences(@ApplicationContext context: Context) = PreferencesSafe(context)

    @Provides
    @Singleton
    fun providesTokenPreferences(@ApplicationContext context: Context) = PreferencesToken(context)

    @Provides
    @Singleton
    fun providesFingerprintHelper(@ApplicationContext context: Context): FingerprintHelper = AndroidFingerprintHelper(context)

    @Provides
    fun providesBip39(wordListProvider: WordListProvider): Bip39 = Bip39Generator(wordListProvider)

    @Provides
    fun providesWordListProvider(@ApplicationContext context: Context): WordListProvider = AndroidWordListProvider(context)

    @Provides
    @Singleton
    fun providesQrCodeGenerator(): QrCodeGenerator = ZxingQrCodeGenerator()

    @Provides
    @Singleton
    fun providesPicasso(): Picasso = Picasso.get()

    @Provides
    @Singleton
    fun providesFirebaseInstanceId() = FirebaseInstanceId.getInstance()

    @Provides
    @Singleton
    fun providesEIP712JsonParser(): EIP712JsonParser = EIP712JsonParser(MoshiAdapter())
}
