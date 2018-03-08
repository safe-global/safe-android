package pm.gnosis.heimdall.common.di.modules

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import okhttp3.Interceptor
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.changeUrl
import javax.inject.Named
import javax.inject.Singleton

@Module
class InterceptorsModule {

    companion object {
        const val REST_CLIENT_INTERCEPTORS = "RestClientInterceptors"
        private const val INTERCEPTORS_WITH_PRIORITY = "InterceptorsWithPriority"
    }

    @Provides
    @Singleton
    @Named(REST_CLIENT_INTERCEPTORS)
    fun providesInterceptorsList(@Named(INTERCEPTORS_WITH_PRIORITY) interceptorMap: @JvmSuppressWildcards Map<Int, Interceptor>): List<Interceptor> {
        return interceptorMap.toSortedMap().map { it.value }
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(0)
    @Named(INTERCEPTORS_WITH_PRIORITY)
    fun providesApiKeyInterceptor(settingsRepository: SettingsRepository): Interceptor {
        return Interceptor {
            var request = it.request()
            if (settingsRepository.needsAuth()) {
                val builder = request.url().newBuilder()
                val url = builder.addQueryParameter("token", BuildConfig.INFURA_API_KEY).build()
                request = request.newBuilder().url(url).build()
            }
            it.proceed(request)
        }
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(1)
    @Named(INTERCEPTORS_WITH_PRIORITY)
    fun providesEndpointInterceptor(settingsRepository: SettingsRepository): Interceptor {
        return Interceptor {
            var request = it.request()
            val url = request.url()
            request = when {
                url.toString().startsWith(BuildConfig.BLOCKCHAIN_NET_URL) ->
                    request.changeUrl(settingsRepository.getEthereumRPCUrl())
                url.toString().startsWith(IpfsApi.BASE_URL) ->
                    request.changeUrl(settingsRepository.getIpfsUrl())
                else -> request
            }
            it.proceed(request)
        }
    }
}
