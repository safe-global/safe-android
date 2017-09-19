package pm.gnosis.android.app.accounts.di

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.android.app.accounts.data.db.AccountsDatabase
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import javax.inject.Singleton

@Module
class AccountsModule {
    @Provides
    @Singleton
    fun providesAccountsDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(context, AccountsDatabase::class.java, AccountsDatabase.DB_NAME).build()
}
