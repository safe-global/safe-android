package pm.gnosis.heimdall.common.di.module

import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.ui.base.BaseContract
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityViewModel

@Module
abstract class ViewModelBindingsModule {
    @Binds
    @ForView
    abstract fun bindsSecurityViewModel(viewModel: SecurityViewModel): SecurityContract.ViewModel

    @Binds
    @ForView
    abstract fun bindsViewModelFactory(viewModel: BaseContract.ViewModelFactory): ViewModelProvider.Factory
}