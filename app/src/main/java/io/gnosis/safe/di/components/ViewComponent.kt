package io.gnosis.safe.di.components

import dagger.Component
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.assets.AssetsFragment
import io.gnosis.safe.ui.assets.coins.CoinsFragment
import io.gnosis.safe.ui.assets.collectibles.CollectiblesFragment
import io.gnosis.safe.ui.assets.collectibles.details.CollectiblesDetailsFragment
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.safe.add.AddSafeFragment
import io.gnosis.safe.ui.safe.add.AddSafeNameFragment
import io.gnosis.safe.ui.safe.add.AddSafeOwnerFragment
import io.gnosis.safe.ui.safe.selection.SafeSelectionDialog
import io.gnosis.safe.ui.safe.share.ShareSafeDialog
import io.gnosis.safe.ui.settings.SettingsFragment
import io.gnosis.safe.ui.settings.app.*
import io.gnosis.safe.ui.settings.app.fiat.AppFiatFragment
import io.gnosis.safe.ui.settings.app.passcode.CreatePasscodeFragment
import io.gnosis.safe.ui.settings.app.passcode.PasscodeSettingsFragment
import io.gnosis.safe.ui.settings.app.passcode.RepeatPasscodeFragment
import io.gnosis.safe.ui.settings.owner.OwnerSeedPhraseFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoFragment
import io.gnosis.safe.ui.settings.owner.list.OwnerSelectionFragment
import io.gnosis.safe.ui.settings.safe.AdvancedSafeSettingsFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsEditNameFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsFragment
import io.gnosis.safe.ui.splash.SplashActivity
import io.gnosis.safe.ui.terms.TermsBottomSheetDialog
import io.gnosis.safe.ui.transactions.TransactionListFragment
import io.gnosis.safe.ui.transactions.TransactionsFragment
import io.gnosis.safe.ui.transactions.details.*

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {

    fun inject(activity: SplashActivity)

    fun inject(activity: StartActivity)

    // Fragments

    fun inject(fragment: AddSafeFragment)

    fun inject(fragment: AddSafeNameFragment)

    fun inject(fragment: AddSafeOwnerFragment)

    fun inject(fragment: TransactionsFragment)

    fun inject(fragment: TransactionListFragment)

    fun inject(fragment: TransactionDetailsFragment)

    fun inject(fragment: TransactionDetailsActionFragment)

    fun inject(fragment: TransactionDetailsActionMultisendFragment)

    fun inject(fragment: AdvancedTransactionDetailsFragment)

    fun inject(fragment: ConfirmRejectionFragment)

    fun inject(dialog: EnsInputDialog)

    fun inject(fragment: SettingsFragment)

    fun inject(fragment: OwnerInfoFragment)

    fun inject(fragment: OwnerSelectionFragment)

    fun inject(fragment: OwnerSeedPhraseFragment)

    fun inject(fragment: AppSettingsFragment)

    fun inject(fragment: GetInTouchFragment)

    fun inject(fragment: AdvancedAppSettingsFragment)

    fun inject(fragment: SafeSettingsFragment)

    fun inject(fragment: SafeSettingsEditNameFragment)

    fun inject(fragment: AssetsFragment)

    fun inject(fragment: CoinsFragment)

    fun inject(fragment: CollectiblesFragment)

    fun inject(fragment: CollectiblesDetailsFragment)

    fun inject(fragment: AdvancedSafeSettingsFragment)

    fun inject(fragment: NightModeAppSettingsFragment)

    fun inject(fragment: AppFiatFragment)

    fun inject(fragment: PasscodeSettingsFragment)

    fun inject(fragment: CreatePasscodeFragment)

    fun inject(fragment: RepeatPasscodeFragment)

    // Dialogs
    fun inject(dialog: SafeSelectionDialog)

    fun inject(dialog: TermsBottomSheetDialog)

    fun inject(dialog: ShareSafeDialog)

    fun inject(creationTransactionDetailsFragment: CreationTransactionDetailsFragment)
}
