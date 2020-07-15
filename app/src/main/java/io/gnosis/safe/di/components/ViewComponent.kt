package io.gnosis.safe.di.components

import dagger.Component
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.safe.add.AddSafeFragment
import io.gnosis.safe.ui.safe.add.AddSafeNameFragment
import io.gnosis.safe.ui.assets.AssetsFragment
import io.gnosis.safe.ui.assets.coins.CoinsFragment
import io.gnosis.safe.ui.assets.collectibles.CollectiblesFragment
import io.gnosis.safe.ui.safe.selection.SafeSelectionDialog
import io.gnosis.safe.ui.settings.SettingsFragment
import io.gnosis.safe.ui.settings.app.AdvancedAppSettingsFragment
import io.gnosis.safe.ui.settings.app.AppSettingsFragment
import io.gnosis.safe.ui.settings.app.GetInTouchFragment
import io.gnosis.safe.ui.settings.safe.AdvancedSafeSettingsFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsFragment
import io.gnosis.safe.ui.safe.share.ShareSafeDialog
import io.gnosis.safe.ui.terms.TermsBottomSheetDialog
import io.gnosis.safe.ui.splash.SplashActivity
import io.gnosis.safe.ui.transactions.TransactionListFragment
import io.gnosis.safe.ui.transactions.details.TransactionDetailsFragment

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

    fun inject(fragment: TransactionListFragment)

    fun inject(fragment: TransactionDetailsFragment)

    fun inject(dialog: EnsInputDialog)

    fun inject(fragment: SettingsFragment)

    fun inject(fragment: AppSettingsFragment)

    fun inject(fragment: GetInTouchFragment)

    fun inject(fragment: AdvancedAppSettingsFragment)

    fun inject(fragment: SafeSettingsFragment)

    fun inject(fragment: AssetsFragment)

    fun inject(fragment: CoinsFragment)

    fun inject(fragment: CollectiblesFragment)

    fun inject(fragment: AdvancedSafeSettingsFragment)

    // Dialogs
    fun inject(dialog: SafeSelectionDialog)

    fun inject(dialog: TermsBottomSheetDialog)

    fun inject(dialog: ShareSafeDialog)
}
