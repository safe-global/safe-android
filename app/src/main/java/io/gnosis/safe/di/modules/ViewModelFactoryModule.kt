package io.gnosis.safe.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.gnosis.safe.di.ViewModelFactory
import io.gnosis.safe.di.ViewModelKey
import io.gnosis.safe.ui.splash.SplashViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashContract(viewModel: SplashViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
