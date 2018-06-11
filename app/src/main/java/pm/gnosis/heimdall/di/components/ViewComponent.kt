package pm.gnosis.heimdall.di.components

import dagger.Component
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.account.AccountActivity
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.authenticate.AuthenticateActivity
import pm.gnosis.heimdall.ui.credits.BuyCreditsActivity
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsActivity
import pm.gnosis.heimdall.ui.dialogs.fingerprint.FingerprintDialog
import pm.gnosis.heimdall.ui.dialogs.share.ShareSafeAddressDialog
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.onboarding.SetupSafeIntroActivity
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordConfirmActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.safe.add.AddExistingSafeFragment
import pm.gnosis.heimdall.ui.safe.add.DeployNewSafeFragment
import pm.gnosis.heimdall.ui.safe.create.CreateSafeActivity
import pm.gnosis.heimdall.ui.safe.create.PairingActivity
import pm.gnosis.heimdall.ui.safe.create.SafeRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsActivity
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.safe.main.PendingSafeFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockDialog
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsActivity
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsActivity
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordActivity
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicActivity
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementActivity
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.add.AddTokenActivity
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoActivity
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferActivity
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionActivity
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {
    // Fragments

    fun inject(fragment: AddExistingSafeFragment)
    fun inject(fragment: DeployNewSafeFragment)
    fun inject(fragment: PendingSafeFragment)
    fun inject(fragment: SafeDetailsFragment)
    fun inject(fragment: SafeTransactionsFragment)
    fun inject(fragment: TokenBalancesFragment)

    // Activities

    fun inject(activity: AccountActivity)
    fun inject(activity: AddressBookActivity)
    fun inject(activity: AddressBookAddEntryActivity)
    fun inject(activity: AddressBookEntryDetailsActivity)
    fun inject(activity: AddTokenActivity)
    fun inject(activity: AuthenticateActivity)
    fun inject(activity: BuyCreditsActivity)
    fun inject(activity: ChangePasswordActivity)
    fun inject(activity: ConfirmTransactionActivity)
    fun inject(activity: CreateAssetTransferActivity)
    fun inject(activity: CreateSafeActivity)
    fun inject(activity: FingerprintSetupActivity)
    fun inject(activity: NetworkSettingsActivity)
    fun inject(activity: DebugSettingsActivity)
    fun inject(activity: PairingActivity)
    fun inject(activity: PasswordConfirmActivity)
    fun inject(activity: PasswordSetupActivity)
    fun inject(activity: QRCodeScanActivity)
    fun inject(activity: RevealMnemonicActivity)
    fun inject(activity: ReviewTransactionActivity)
    fun inject(activity: SafeMainActivity)
    fun inject(activity: SafeRecoveryPhraseActivity)
    fun inject(activity: SafeSettingsActivity)
    fun inject(activity: SecuritySettingsActivity)
    fun inject(activity: SelectSafeActivity)
    fun inject(activity: SetupSafeIntroActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TokenManagementActivity)
    fun inject(activity: TokenInfoActivity)
    fun inject(activity: TransactionStatusActivity)
    fun inject(activity: UnlockActivity)

    // Dialogs

    fun inject(dialog: FingerprintDialog)
    fun inject(dialog: ShareSafeAddressDialog)
    fun inject(dialog: SimpleAddressShareDialog)
    fun inject(dialog: UnlockDialog)
}
