package pm.gnosis.android.app.accounts.di

import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.KethereumAccountsRepository
import pm.gnosis.android.app.authenticator.data.PreferencesManager
import javax.inject.Singleton

@Module
class AccountsModule(val context: Context) {

    @Provides
    @Singleton
    fun providesAccountRepository(preferencesManager: PreferencesManager): AccountsRepository
            = KethereumAccountsRepository(preferencesManager)
}