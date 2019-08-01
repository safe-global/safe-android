package pm.gnosis.heimdall.di.modules

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
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
            val url = builder.addPathSegment(BuildConfig.INFURA_API_KEY).build()
            it.proceed(request.newBuilder().url(url).build())
        }
    }

    @Provides
    @Singleton
    @IntoMap
    @IntKey(1)
    @Named(INTERCEPTORS_WITH_PRIORITY)
    fun providesLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = if(BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }
}
