package io.gnosis.safe.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import io.gnosis.data.db.SafeDatabase
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.safe.di.ApplicationContext
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideSafeDatabase(@ApplicationContext context: Context): SafeDatabase =
        Room.databaseBuilder(context, SafeDatabase::class.java, SafeDatabase.DB_NAME).build()

    @Provides
    @Singleton
    fun provideSafeDao(safeDatabase: SafeDatabase): SafeDao = safeDatabase.safeDao()
}
