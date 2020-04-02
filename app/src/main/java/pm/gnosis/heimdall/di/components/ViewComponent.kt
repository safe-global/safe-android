package pm.gnosis.heimdall.di.components

import dagger.Component
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.splash.SplashActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)

interface ViewComponent {
    fun inject(activity: SplashActivity)
}
