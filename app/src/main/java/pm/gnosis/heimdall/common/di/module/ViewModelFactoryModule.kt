package pm.gnosis.heimdall.common.di.module

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
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoContract
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoViewModel
import pm.gnosis.heimdall.ui.multisig.overview.MultisigOverviewContract
import pm.gnosis.heimdall.ui.multisig.overview.MultisigOverviewViewModel
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicViewModel
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountContract
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountViewModel
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.SplashViewModel
import pm.gnosis.heimdall.ui.tokens.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.AddTokenViewModel
import pm.gnosis.heimdall.ui.tokens.TokensContract
import pm.gnosis.heimdall.ui.tokens.TokensViewModel
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
    @ViewModelKey(MultisigInfoContract::class)
    abstract fun bindsMultisigDetailsContract(viewModel: MultisigInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MultisigOverviewContract::class)
    abstract fun bindsMultisigOverviewContract(viewModel: MultisigOverviewViewModel): ViewModel

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
