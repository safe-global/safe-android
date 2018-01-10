package pm.gnosis.ticker.di

import android.arch.persistence.room.Room
import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.ticker.data.db.TickerDatabase
import pm.gnosis.ticker.data.remote.TickerApi
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
class TickerModule {
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
    fun providesTickerDb(@ApplicationContext context: Context) = Room.databaseBuilder(context, TickerDatabase::class.java, TickerDatabase.DB_NAME).build()
}
