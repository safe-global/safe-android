package pm.gnosis.heimdall.common.di.module

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import pm.gnosis.heimdall.common.di.ViewModelFactory
import pm.gnosis.heimdall.common.di.ViewModelKey
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicPresenter
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {
    @Binds
    @IntoMap
    @ViewModelKey(SecurityContract.ViewModel::class)
    abstract fun bindsSecurityViewModel(viewModel: SecurityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GenerateMnemonicContract::class)
    abstract fun bindsGenerateMnemonicPresenter(viewModel: GenerateMnemonicPresenter): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
