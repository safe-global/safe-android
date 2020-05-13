package io.gnosis.safe.di.components

import dagger.Component
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.safe.add.AddSafeFragment
import io.gnosis.safe.ui.safe.add.AddSafeNameFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.safe.balances.SafeBalancesFragment
import io.gnosis.safe.ui.safe.balances.coins.CoinsFragment
import io.gnosis.safe.ui.safe.balances.collectibles.CollectiblesFragment
import io.gnosis.safe.ui.safe.selection.SafeSelectionDialog
import io.gnosis.safe.ui.safe.settings.SafeSettingsFragment
import io.gnosis.safe.ui.splash.SplashActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {

    fun inject(activity: SplashActivity)

    // Fragments

    fun inject(fragment: AddSafeFragment)

    fun inject(fragment: AddSafeNameFragment)

    fun inject(dialog: EnsInputDialog)

    fun inject(fragment: NoSafeFragment)

    fun inject(fragment: SafeBalancesFragment)

    fun inject(fragment: SafeSettingsFragment)

    fun inject(fragment: CoinsFragment)

    fun inject(fragment: CollectiblesFragment)

    // Dialogs
    fun inject(dialog: SafeSelectionDialog)
}
