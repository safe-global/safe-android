package pm.gnosis.heimdall.common.di.modules

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import pm.gnosis.heimdall.common.di.ViewModelFactory
import pm.gnosis.heimdall.common.di.ViewModelKey
import pm.gnosis.heimdall.ui.account.AccountContract
import pm.gnosis.heimdall.ui.account.AccountViewModel
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.addressbook.AddressBookViewModel
import pm.gnosis.heimdall.ui.authenticate.AuthenticateContract
import pm.gnosis.heimdall.ui.authenticate.AuthenticateViewModel
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicViewModel
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountContract
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountViewModel
import pm.gnosis.heimdall.ui.safe.add.AddSafeContract
import pm.gnosis.heimdall.ui.safe.add.AddSafeViewModel
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsViewModel
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoContract
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoViewModel
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsViewModel
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewContract
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsContract
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsViewModel
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementContract
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementViewModel
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.SplashViewModel
import pm.gnosis.heimdall.ui.tokens.add.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.add.AddTokenViewModel
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesViewModel
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoContract
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoViewModel
import pm.gnosis.heimdall.ui.transactions.BaseTransactionContract
import pm.gnosis.heimdall.ui.transactions.BaseTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract
import pm.gnosis.heimdall.ui.transactions.ViewTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.details.*
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {
    @Binds
    @IntoMap
    @ViewModelKey(AccountContract::class)
    abstract fun bindsAccountContract(viewModel: AccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddressBookContract::class)
    abstract fun bindsAddressBookContract(viewModel: AddressBookViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeContract::class)
    abstract fun bindsAddSafeContract(viewModel: AddSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddTokenContract::class)
    abstract fun bindsAddTokenContract(viewModel: AddTokenViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssetTransferTransactionDetailsContract::class)
    abstract fun bindsAssetTransferTransactionDetailsContract(viewModel: AssetTransferTransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AuthenticateContract::class)
    abstract fun bindsAuthenticateContract(viewModel: AuthenticateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseTransactionContract::class)
    abstract fun bindsBaseTransactionContract(viewModel: BaseTransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseTransactionDetailsContract::class)
    abstract fun bindsBaseTransactionDetailsContract(viewModel: BaseTransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GenerateMnemonicContract::class)
    abstract fun bindsGenerateMnemonicContract(viewModel: GenerateMnemonicViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GenericTransactionDetailsContract::class)
    abstract fun bindsGenericTransactionDetailsContract(viewModel: GenericTransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkSettingsContract::class)
    abstract fun bindsNetworkSettingsContract(viewModel: NetworkSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RestoreAccountContract::class)
    abstract fun bindsRestoreAccountContract(viewModel: RestoreAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeDetailsContract::class)
    abstract fun bindsSafeDetailsContract(viewModel: SafeDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeInfoContract::class)
    abstract fun bindsSafeInfoContract(viewModel: SafeInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeOverviewContract::class)
    abstract fun bindsSafeOverviewContract(viewModel: SafeOverviewViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeTransactionsContract::class)
    abstract fun bindsSafeTransactionsContract(viewModel: SafeTransactionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SecurityContract::class)
    abstract fun bindsSecurityContract(viewModel: SecurityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashContract::class)
    abstract fun bindsSplashContract(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokenBalancesContract::class)
    abstract fun bindsTokensContract(viewModel: TokenBalancesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokenInfoContract::class)
    abstract fun bindsTokensInfoContract(viewModel: TokenInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokenManagementContract::class)
    abstract fun bindsTokenManagementContract(viewModel: TokenManagementViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ViewTransactionContract::class)
    abstract fun bindsViewTransactionContract(viewModel: ViewTransactionViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
