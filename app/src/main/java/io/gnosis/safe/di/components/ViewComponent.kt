package io.gnosis.safe.di.components

import dagger.Component
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.safe.add.AddSafeFragment
import io.gnosis.safe.ui.safe.add.AddSafeNameFragment
import io.gnosis.safe.ui.safe.overview.SafeOverviewFragment
import io.gnosis.safe.ui.safe.NoSafeFragment
import io.gnosis.safe.ui.safe.SafeBalancesFragment
import io.gnosis.safe.ui.safe.SafeOverviewFragment
import io.gnosis.safe.ui.safe.SafeSelectionDialog
import io.gnosis.safe.ui.splash.SplashActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {

    fun inject(activity: SplashActivity)

    // Fragments

    fun inject(fragment: SafeOverviewFragment)

    fun inject(fragment: AddSafeFragment)

    fun inject(fragment: AddSafeNameFragment)

    fun inject(dialog: EnsInputDialog)

    fun inject(fragment: NoSafeFragment)

    fun inject(fragment: SafeBalancesFragment)

    // Dialogs
    fun inject(dialog: SafeSelectionDialog)
}
