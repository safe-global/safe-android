package pm.gnosis.heimdall.di.modules

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import okhttp3.Interceptor
import pm.gnosis.heimdall.BuildConfig
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
    fun providesApiKeyInterceptor(): Interceptor {
        return Interceptor {
            val request = it.request()
            val builder = request.url().newBuilder()
            val url = builder.addQueryParameter("token", BuildConfig.INFURA_API_KEY).build()
            it.proceed(request.newBuilder().url(url).build())
        }
    }
}
