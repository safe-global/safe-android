package io.gnosis.safe.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import io.gnosis.data.db.HeimdallDatabase
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.safe.di.ApplicationContext
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideSafeDatabase(@ApplicationContext context: Context): HeimdallDatabase =
        Room.databaseBuilder(context, HeimdallDatabase::class.java, HeimdallDatabase.DB_NAME)
            .build()

    @Provides
    @Singleton
    fun provideSafeDao(heimdallDatabase: HeimdallDatabase): SafeDao = heimdallDatabase.safeDao()

    @Provides
    @Singleton
    fun provideErc20Dao(heimdallDatabase: HeimdallDatabase): Erc20TokenDao = heimdallDatabase.erc20TokenDao()
}
