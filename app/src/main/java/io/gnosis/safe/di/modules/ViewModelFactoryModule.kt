package io.gnosis.safe.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.gnosis.safe.di.ViewModelFactory
import io.gnosis.safe.di.ViewModelKey
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.balances.SafeBalancesViewModel
import io.gnosis.safe.ui.safe.empty.NoSafeViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.gnosis.safe.ui.safe.settings.SafeSettingsViewModel
import io.gnosis.safe.ui.splash.SplashViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeViewModel::class)
    abstract fun bindsAddSafeViewModel(viewModel: AddSafeViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(SafeSelectionViewModel::class)
    abstract fun bindsSafeSelectionViewModel(viewModel: SafeSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NoSafeViewModel::class)
    abstract fun bindsNoSafeViewModel(viewModel: NoSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeBalancesViewModel::class)
    abstract fun bindsSafeBalancesViewModel(viewModel: SafeBalancesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsViewModel::class)
    abstract fun bindsSafeSettingsViewModel(viewModel: SafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel
}
