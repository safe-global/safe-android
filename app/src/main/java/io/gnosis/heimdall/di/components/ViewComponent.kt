package io.gnosis.heimdall.di.components

import dagger.Component
import io.gnosis.heimdall.di.ForView
import io.gnosis.heimdall.di.modules.ViewModule
import io.gnosis.heimdall.ui.splash.SplashActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)

interface ViewComponent {
    fun inject(activity: SplashActivity)
}
