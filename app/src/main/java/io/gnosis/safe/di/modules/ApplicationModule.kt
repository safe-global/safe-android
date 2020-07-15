package io.gnosis.safe.di.modules

import android.app.Application
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.BuildConfig
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.safe.terms.TermsChecker
import io.gnosis.safe.ui.safe.transactions.paging.TransactionPagingProvider
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pm.gnosis.ethereum.rpc.EthereumRpcConnector
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcApi
import pm.gnosis.ethereum.rpc.retrofit.RetrofitEthereumRpcConnector
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.ZxingQrCodeGenerator
import retrofit2.Retrofit
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
    fun providesAppDispatchers(): AppDispatchers = AppDispatchers()

    @Provides
    @Singleton
    fun providesPreferencesManager(@ApplicationContext context: Context): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun providesTermsChecker(preferencesManager: PreferencesManager): TermsChecker = TermsChecker(preferencesManager)

    @Provides
    @Singleton
    fun providesTracker(@ApplicationContext context: Context): Tracker = Tracker.getInstance(context)

    @Provides
    @Singleton
    fun providesEthereumRpcConnector(retrofitEthereumRpcApi: RetrofitEthereumRpcApi): EthereumRpcConnector =
        RetrofitEthereumRpcConnector(retrofitEthereumRpcApi)

    @Provides
    @Singleton
    fun providesMoshi(): Moshi = dataMoshi

    @Provides
    @Singleton
    fun providesEthereumJsonRpcApi(moshi: Moshi, @Named(INFURA_REST_CLIENT) client: OkHttpClient): RetrofitEthereumRpcApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BuildConfig.BLOCKCHAIN_NET_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RetrofitEthereumRpcApi::class.java)

    @Provides
    @Singleton
    fun providesTransactionServiceApi(moshi: Moshi, client: OkHttpClient): TransactionServiceApi =
        Retrofit.Builder()
            .client(client)
            .baseUrl(TransactionServiceApi.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TransactionServiceApi::class.java)

    @Provides
    @Singleton
    @Named(INFURA_REST_CLIENT)
    fun providesInfuraOkHttpClient(
        okHttpClient: OkHttpClient,
        @Named(InterceptorsModule.REST_CLIENT_INTERCEPTORS) interceptors: @JvmSuppressWildcards List<Interceptor>
    ): OkHttpClient =
        okHttpClient.newBuilder().apply {
            interceptors.forEach {
                addInterceptor(it)
            }
        }.build()

    @Provides
    @Singleton
    fun providesOkHttpClient(
        @Named(InterceptorsModule.REST_CLIENT_INTERCEPTORS) interceptors: @JvmSuppressWildcards List<Interceptor>
    ): OkHttpClient =
        OkHttpClient.Builder().apply {
            addInterceptor(interceptors[1])
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
    fun providesTransactionPagingProvider(transactionRepository: TransactionRepository): TransactionPagingProvider =
        TransactionPagingProvider(transactionRepository)

    @Provides
    @Singleton
    fun providesQrCodeGenerator(): QrCodeGenerator = ZxingQrCodeGenerator()
}
