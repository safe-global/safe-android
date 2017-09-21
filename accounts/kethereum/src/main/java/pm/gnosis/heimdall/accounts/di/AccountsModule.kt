package pm.gnosis.heimdall.accounts.di

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.accounts.data.db.AccountsDatabase
import pm.gnosis.heimdall.common.di.ApplicationContext
import javax.inject.Singleton

@Module
class AccountsModule {
    @Provides
    @Singleton
    fun providesAccountsDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(context, AccountsDatabase::class.java, AccountsDatabase.DB_NAME).build()
}
