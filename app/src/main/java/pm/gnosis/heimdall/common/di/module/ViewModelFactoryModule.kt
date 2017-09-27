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
import pm.gnosis.heimdall.ui.multisig.MultisigContract
import pm.gnosis.heimdall.ui.multisig.MultisigViewModel
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicViewModel
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountContract
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountViewModel
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {
    @Binds
    @IntoMap
    @ViewModelKey(AccountContract::class)
    abstract fun bindsAccountContract(viewModel: AccountViewModel): ViewModel

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
    @ViewModelKey(MultisigContract::class)
    abstract fun bindsMultisigContract(viewModel: MultisigViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RestoreAccountContract::class)
    abstract fun bindsRestoreAccountContract(viewModel: RestoreAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SecurityContract::class)
    abstract fun bindsSecurityContract(viewModel: SecurityViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
