package pm.gnosis.heimdall.common.di.module

import dagger.Binds
import dagger.Module
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.security.SecurityPresenter

@Module
abstract class PresenterBindingsModule {
    @Binds
    @ForView
    abstract fun bindsSecurityPresenter(presenter: SecurityPresenter): SecurityContract.Presenter
}