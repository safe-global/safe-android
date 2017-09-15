package pm.gnosis.android.app.accounts.di

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.android.app.accounts.data.db.AccountsDb
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import javax.inject.Singleton

@Module
class AccountsModule {
    @Provides
    @Singleton
    fun providesAccountsDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(context, AccountsDb::class.java, AccountsDb.DB_NAME).build()
}
