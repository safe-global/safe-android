package pm.gnosis.heimdall.common.di.components

import dagger.Component
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.ui.account.AccountActivity
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.authenticate.AuthenticateActivity
import pm.gnosis.heimdall.ui.credits.BuyCreditsActivity
import pm.gnosis.heimdall.ui.dialogs.fingerprint.FingerprintDialog
import pm.gnosis.heimdall.ui.dialogs.share.RequestSignatureDialog
import pm.gnosis.heimdall.ui.dialogs.share.ShareSafeAddressDialog
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateAddExtensionTransactionProgressDialog
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateTokenTransactionProgressDialog
import pm.gnosis.heimdall.ui.onboarding.SetupSafeIntroActivity
import pm.gnosis.heimdall.ui.onboarding.account.AccountSetupActivity
import pm.gnosis.heimdall.ui.onboarding.account.create.GenerateMnemonicActivity
import pm.gnosis.heimdall.ui.onboarding.account.restore.RestoreAccountActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordConfirmActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.safe.add.AddExistingSafeFragment
import pm.gnosis.heimdall.ui.safe.add.DeployNewSafeFragment
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsActivity
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.safe.main.PendingSafeFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsActivity
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsActivity
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordActivity
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicActivity
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementActivity
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.add.AddTokenActivity
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoActivity
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.heimdall.ui.transactions.ReceiptTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SignTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SubmitTransactionActivity
import pm.gnosis.heimdall.ui.transactions.details.assets.CreateAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.assets.ViewAssetTransferDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.extensions.recovery.CreateAddRecoveryExtensionFragment
import pm.gnosis.heimdall.ui.transactions.details.extensions.recovery.ViewAddRecoveryExtensionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.generic.CreateGenericTransactionDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.CreateAddOwnerDetailsFragment
import pm.gnosis.heimdall.ui.transactions.details.safe.ViewChangeSafeSettingsDetailsFragment
import pm.gnosis.svalinn.common.di.ForView

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {
    // Fragments

    fun inject(fragment: AddExistingSafeFragment)
    fun inject(fragment: CreateAssetTransferDetailsFragment)
    fun inject(fragment: CreateAddOwnerDetailsFragment)
    fun inject(fragment: CreateAddRecoveryExtensionFragment)
    fun inject(fragment: CreateGenericTransactionDetailsFragment)
    fun inject(fragment: DeployNewSafeFragment)
    fun inject(fragment: PendingSafeFragment)
    fun inject(fragment: SafeDetailsFragment)
    fun inject(fragment: SafeTransactionsFragment)
    fun inject(fragment: TokenBalancesFragment)
    fun inject(fragment: ViewAddRecoveryExtensionDetailsFragment)
    fun inject(fragment: ViewAssetTransferDetailsFragment)
    fun inject(fragment: ViewChangeSafeSettingsDetailsFragment)

    // Activities

    fun inject(activity: AccountActivity)
    fun inject(activity: AccountSetupActivity)
    fun inject(activity: AddressBookActivity)
    fun inject(activity: AddressBookAddEntryActivity)
    fun inject(activity: AddressBookEntryDetailsActivity)
    fun inject(activity: AddTokenActivity)
    fun inject(activity: AuthenticateActivity)
    fun inject(activity: BuyCreditsActivity)
    fun inject(activity: ChangePasswordActivity)
    fun inject(activity: CreateTransactionActivity)
    fun inject(activity: GenerateMnemonicActivity)
    fun inject(activity: NetworkSettingsActivity)
    fun inject(activity: PasswordConfirmActivity)
    fun inject(activity: PasswordSetupActivity)
    fun inject(activity: QRCodeScanActivity)
    fun inject(activity: ReceiptTransactionActivity)
    fun inject(activity: RestoreAccountActivity)
    fun inject(activity: RevealMnemonicActivity)
    fun inject(activity: SafeMainActivity)
    fun inject(activity: SafeSettingsActivity)
    fun inject(activity: SafesOverviewActivity)
    fun inject(activity: SecuritySettingsActivity)
    fun inject(activity: SelectSafeActivity)
    fun inject(activity: SetupSafeIntroActivity)
    fun inject(activity: SignTransactionActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: SubmitTransactionActivity)
    fun inject(activity: TokenManagementActivity)
    fun inject(activity: TokenInfoActivity)
    fun inject(activity: UnlockActivity)

    // Dialogs

    fun inject(dialog: CreateAddExtensionTransactionProgressDialog)
    fun inject(dialog: CreateTokenTransactionProgressDialog)
    fun inject(dialog: FingerprintDialog)
    fun inject(dialog: RequestSignatureDialog)
    fun inject(dialog: ShareSafeAddressDialog)
    fun inject(dialog: SimpleAddressShareDialog)
}
