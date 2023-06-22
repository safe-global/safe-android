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
import io.gnosis.safe.ui.dialogs.UnstoppableInputDialog
import io.gnosis.safe.ui.safe.add.AddSafeFragment
import io.gnosis.safe.ui.safe.add.AddSafeNameFragment
import io.gnosis.safe.ui.safe.add.AddSafeOwnerFragment
import io.gnosis.safe.ui.safe.selection.SafeSelectionDialog
import io.gnosis.safe.ui.safe.send_funds.*
import io.gnosis.safe.ui.safe.share.ShareSafeDialog
import io.gnosis.safe.ui.settings.SettingsFragment
import io.gnosis.safe.ui.settings.app.*
import io.gnosis.safe.ui.settings.app.fiat.AppFiatFragment
import io.gnosis.safe.ui.settings.app.passcode.*
import io.gnosis.safe.ui.settings.chain.ChainSelectionFragment
import io.gnosis.safe.ui.settings.owner.OwnerAddOptionsFragment
import io.gnosis.safe.ui.settings.owner.OwnerEditNameFragment
import io.gnosis.safe.ui.settings.owner.OwnerEnterNameFragment
import io.gnosis.safe.ui.settings.owner.OwnerSeedPhraseFragment
import io.gnosis.safe.ui.settings.owner.details.OwnerDetailsFragment
import io.gnosis.safe.ui.settings.owner.export.OwnerExportFragment
import io.gnosis.safe.ui.settings.owner.export.OwnerExportKeyFragment
import io.gnosis.safe.ui.settings.owner.export.OwnerExportSeedFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoGenerateFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoKeystoneFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoLedgerFragment
import io.gnosis.safe.ui.settings.owner.keystone.KeystoneRequestSignatureFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerOwnerSelectionFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerTabsFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerDeviceListFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerSignDialog
import io.gnosis.safe.ui.settings.owner.list.OwnerListFragment
import io.gnosis.safe.ui.settings.owner.keystone.KeystoneOwnerSelectionFragment
import io.gnosis.safe.ui.settings.owner.selection.OwnerSelectionFragment
import io.gnosis.safe.ui.settings.safe.AdvancedSafeSettingsFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsEditNameFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsFragment
import io.gnosis.safe.ui.splash.SplashActivity
import io.gnosis.safe.ui.terms.TermsBottomSheetDialog
import io.gnosis.safe.ui.transactions.TransactionListFragment
import io.gnosis.safe.ui.transactions.TransactionsFragment
import io.gnosis.safe.ui.transactions.details.*
import io.gnosis.safe.ui.transactions.execution.TxAdvancedParamsFragment
import io.gnosis.safe.ui.transactions.execution.TxEditFee1559Fragment
import io.gnosis.safe.ui.transactions.execution.TxEditFeeLegacyFragment
import io.gnosis.safe.ui.transactions.execution.TxSuccessFragment
import io.gnosis.safe.ui.updates.UpdatesFragment
import io.gnosis.safe.ui.whatsnew.WhatsNewDialog

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {

    fun inject(activity: SplashActivity)

    fun inject(activity: StartActivity)

    // Fragments

    fun inject(fragment: ChainSelectionFragment)

    fun inject(fragment: AddSafeFragment)

    fun inject(fragment: AddSafeNameFragment)

    fun inject(fragment: AddSafeOwnerFragment)

    fun inject(fragment: TransactionsFragment)

    fun inject(fragment: TransactionListFragment)

    fun inject(fragment: TransactionDetailsFragment)

    fun inject(creationTransactionDetailsFragment: CreationTransactionDetailsFragment)

    fun inject(fragment: TransactionDetailsActionFragment)

    fun inject(fragment: TransactionDetailsActionMultisendFragment)

    fun inject(fragment: AdvancedTransactionDetailsFragment)

    fun inject(fragment: TxEditFee1559Fragment)

    fun inject(fragment: TxEditFeeLegacyFragment)

    fun inject(fragment: TxAdvancedParamsFragment)

    fun inject(fragment: ConfirmRejectionFragment)

    fun inject(fragment: TxSuccessFragment)

    fun inject(fragment: SettingsFragment)

    fun inject(fragment: OwnerAddOptionsFragment)

    fun inject(fragment: OwnerInfoFragment)

    fun inject(fragment: OwnerInfoGenerateFragment)

    fun inject(fragment: OwnerInfoLedgerFragment)

    fun inject(fragment: OwnerInfoKeystoneFragment)

    fun inject(fragment: OwnerSelectionFragment)

    fun inject(fragment: LedgerOwnerSelectionFragment)

    fun inject(fragment: KeystoneOwnerSelectionFragment)

    fun inject(fragment: LedgerTabsFragment)

    fun inject(fragment: OwnerSeedPhraseFragment)

    fun inject(fragment: OwnerEnterNameFragment)

    fun inject(fragment: OwnerEditNameFragment)

    fun inject(fragment: OwnerListFragment)

    fun inject(fragment: OwnerDetailsFragment)

    fun inject(fragment: OwnerExportFragment)

    fun inject(fragment: OwnerExportKeyFragment)

    fun inject(fragment: OwnerExportSeedFragment)

    fun inject(fragment: AppSettingsFragment)

    fun inject(fragment: ChainPrefixAppSettingsFragment)

    fun inject(fragment: GetInTouchFragment)

    fun inject(fragment: AboutSafeFragment)

    fun inject(fragment: AdvancedAppSettingsFragment)

    fun inject(fragment: SafeSettingsFragment)

    fun inject(fragment: SafeSettingsEditNameFragment)

    fun inject(fragment: AssetsFragment)

    fun inject(fragment: AssetSelectionFragment)

    fun inject(fragment: SendAssetFragment)

    fun inject(fragment: SendAssetReviewFragment)

    fun inject(fragment: EditAdvancedParamsFragment)

    fun inject(fragment: SendAssetSuccessFragment)

    fun inject(fragment: AddOwnerFirstFragment)

    fun inject(fragment: CoinsFragment)

    fun inject(fragment: CollectiblesFragment)

    fun inject(fragment: CollectiblesDetailsFragment)

    fun inject(fragment: AdvancedSafeSettingsFragment)

    fun inject(fragment: NightModeAppSettingsFragment)

    fun inject(fragment: AppFiatFragment)

    fun inject(fragment: PasscodeSettingsFragment)

    fun inject(fragment: CreatePasscodeFragment)

    fun inject(fragment: RepeatPasscodeFragment)

    fun inject(fragment: ConfigurePasscodeFragment)

    fun inject(fragment: ChangePasscodeFragment)

    fun inject(fragment: ChangeCreatePasscodeFragment)

    fun inject(fragment: ChangeRepeatPasscodeFragment)

    fun inject(fragment: EnterPasscodeFragment)

    fun inject(fragment: SigningOwnerSelectionFragment)

    fun inject(fragment: UpdatesFragment)

    fun inject(fragment: LedgerDeviceListFragment)

    fun inject(fragment: KeystoneRequestSignatureFragment)

    // Dialogs
    fun inject(dialog: EnsInputDialog)

    fun inject(dialog: UnstoppableInputDialog)

    fun inject(dialog: SafeSelectionDialog)

    fun inject(dialog: TermsBottomSheetDialog)

    fun inject(dialog: ShareSafeDialog)

    fun inject(dialog: LedgerSignDialog)

    fun inject(dialog: WhatsNewDialog)
}
