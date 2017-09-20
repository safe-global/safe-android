package pm.gnosis.heimdall.accounts.di

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.repositories.impl.KethereumAccountsRepository

@Module
abstract class AccountsBindingModule {
    @Binds
    abstract fun bindAccountsRepository(kethereumAccountsRepository: KethereumAccountsRepository): AccountsRepository
}
