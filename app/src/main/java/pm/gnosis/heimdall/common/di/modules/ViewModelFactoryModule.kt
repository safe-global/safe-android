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
import pm.gnosis.heimdall.ui.authenticate.AuthenticateContract
import pm.gnosis.heimdall.ui.authenticate.AuthenticateViewModel
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsViewModel
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoContract
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoViewModel
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewContract
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicViewModel
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountContract
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountViewModel
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsViewModel
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel
import pm.gnosis.heimdall.ui.settings.SettingsContract
import pm.gnosis.heimdall.ui.settings.SettingsViewModel
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.SplashViewModel
import pm.gnosis.heimdall.ui.tokens.addtoken.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.addtoken.AddTokenViewModel
import pm.gnosis.heimdall.ui.tokens.overview.TokensContract
import pm.gnosis.heimdall.ui.tokens.overview.TokensViewModel
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsContract
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {
    @Binds
    @IntoMap
    @ViewModelKey(AccountContract::class)
    abstract fun bindsAccountContract(viewModel: AccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddTokenContract::class)
    abstract fun bindsAddTokenContract(viewModel: AddTokenViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AuthenticateContract::class)
    abstract fun bindsAuthenticateContract(viewModel: AuthenticateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GenerateMnemonicContract::class)
    abstract fun bindsGenerateMnemonicContract(viewModel: GenerateMnemonicViewModel): ViewModel

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
    @ViewModelKey(RestoreAccountContract::class)
    abstract fun bindsRestoreAccountContract(viewModel: RestoreAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SecurityContract::class)
    abstract fun bindsSecurityContract(viewModel: SecurityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsContract::class)
    abstract fun bindsSettingsContract(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashContract::class)
    abstract fun bindsSplashContract(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokensContract::class)
    abstract fun bindsTokensContract(viewModel: TokensViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailsContract::class)
    abstract fun bindsTransactionDetailsContract(viewModel: TransactionDetailsViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
