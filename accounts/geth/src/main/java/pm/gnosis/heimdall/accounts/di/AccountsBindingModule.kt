package pm.gnosis.heimdall.accounts.di

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.repositories.impl.GethAccountsRepository

@Module
abstract class AccountsBindingModule {
    @Binds
    abstract fun bindAccountsRepository(gethAccountsRepository: GethAccountsRepository): AccountsRepository
}
