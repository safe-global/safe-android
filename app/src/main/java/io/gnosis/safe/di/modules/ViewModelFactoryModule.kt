package io.gnosis.safe.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.ViewModelFactory
import io.gnosis.safe.di.ViewModelKey
import io.gnosis.safe.ui.dialogs.EnsInputViewModel
import io.gnosis.safe.ui.safe.add.AddSafeNameViewModel
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.balances.SafeBalancesViewModel
import io.gnosis.safe.ui.safe.balances.coins.CoinsViewModel
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
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EnsInputViewModel::class)
    abstract fun providesEnsInputViewModel(viewModel: EnsInputViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeNameViewModel::class)
    abstract fun providesAddSafeNameViewModel(viewModel: AddSafeNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CoinsViewModel::class)
    abstract fun providesCoinsViewModel(viewModel: CoinsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeBalancesViewModel::class)
    abstract fun providesSafeBalancesViewModel(viewModel: SafeBalancesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NoSafeViewModel::class)
    abstract fun providesNoSafeViewModel(viewModel: NoSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSelectionViewModel::class)
    abstract fun providesSafeSelectionViewModel(viewModel: SafeSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsViewModel::class)
    abstract fun providesSafeSettingsViewModel(viewModel: SafeSettingsViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
