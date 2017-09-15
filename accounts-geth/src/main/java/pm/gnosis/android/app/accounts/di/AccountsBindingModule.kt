package pm.gnosis.android.app.accounts.di

import dagger.Binds
import dagger.Module
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.GethAccountsRepository

@Module
abstract class AccountsBindingModule {
    @Binds
    abstract fun bindAccountsRepository(gethAccountsRepository: GethAccountsRepository): AccountsRepository
}
