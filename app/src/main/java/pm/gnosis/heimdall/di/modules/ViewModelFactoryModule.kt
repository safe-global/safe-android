package pm.gnosis.heimdall.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.di.ViewModelFactory
import pm.gnosis.heimdall.di.ViewModelKey
import pm.gnosis.heimdall.ui.splash.SplashViewModel
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
