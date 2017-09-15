package pm.gnosis.android.app.accounts.di

import dagger.Binds
import dagger.Module
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.KethereumAccountsRepository
import javax.inject.Singleton

@Module
abstract class AccountsBindingModule {
    @Binds
    @Singleton
    abstract fun bindAccountsRepository(kethereumAccountsRepository: KethereumAccountsRepository): AccountsRepository
}
