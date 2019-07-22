package pm.gnosis.heimdall.di.components

import dagger.Component
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.addressbook.edit.AddressBookEditEntryActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsActivity
import pm.gnosis.heimdall.ui.dialogs.ens.EnsInputDialog
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.messagesigning.SignatureRequestActivity
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordConfirmActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.heimdall.ui.safe.connect.ConnectExtensionActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafePaymentTokenActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeSetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.safe.main.NoSafesFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.pairing.PairingActivity
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressFragment
import pm.gnosis.heimdall.ui.safe.pending.SafeCreationFundFragment
import pm.gnosis.heimdall.ui.safe.recover.extension.ReplaceExtensionRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.extension.ReplaceExtensionSubmitActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ConfirmNewRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ScanExtensionAddressActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.SetupNewRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeFundFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafePendingFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeSubmitFragment
import pm.gnosis.heimdall.ui.security.unlock.UnlockActivity
import pm.gnosis.heimdall.ui.security.unlock.UnlockDialog
import pm.gnosis.heimdall.ui.settings.general.GeneralSettingsActivity
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordEnterNewFragment
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordEnterOldFragment
import pm.gnosis.heimdall.ui.settings.general.changepassword.PasswordChangeActivity
import pm.gnosis.heimdall.ui.settings.general.fingerprint.FingerprintDialog
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesFragment
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensActivity
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenActivity
import pm.gnosis.heimdall.ui.tokens.select.SelectTokenActivity
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferActivity
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionActivity
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusActivity
import pm.gnosis.heimdall.ui.walletconnect.intro.WalletConnectIntroActivity
import pm.gnosis.heimdall.ui.walletconnect.link.WalletConnectLinkActivity
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsActivity

@ForView
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ViewModule::class]
)
interface ViewComponent {
    // Fragments

    fun inject(fragment: DeploySafeProgressFragment)
    fun inject(fragment: NoSafesFragment)
    fun inject(fragment: RecoveringSafeFragment)
    fun inject(fragment: RecoveringSafeFundFragment)
    fun inject(fragment: RecoveringSafePendingFragment)
    fun inject(fragment: RecoveringSafeSubmitFragment)
    fun inject(fragment: SafeCreationFundFragment)
    fun inject(fragment: SafeDetailsFragment)
    fun inject(fragment: SafeTransactionsFragment)
    fun inject(fragment: TokenBalancesFragment)
    fun inject(fragment: ChangePasswordEnterOldFragment)
    fun inject(fragment: ChangePasswordEnterNewFragment)

    // Activities

    fun inject(activity: AddressBookActivity)
    fun inject(activity: AddressBookAddEntryActivity)
    fun inject(activity: AddressBookEditEntryActivity)
    fun inject(activity: AddressBookEntryDetailsActivity)
    fun inject(activity: CheckSafeActivity)
    fun inject(activity: ConfirmTransactionActivity)
    fun inject(activity: ConfirmNewRecoveryPhraseActivity)
    fun inject(activity: ConnectExtensionActivity)
    fun inject(activity: CreateAssetTransferActivity)
    fun inject(activity: CreateSafeIntroActivity)
    fun inject(activity: CreateSafeConfirmRecoveryPhraseActivity)
    fun inject(activity: CreateSafePaymentTokenActivity)
    fun inject(activity: CreateSafeSetupRecoveryPhraseActivity)
    fun inject(activity: DebugSettingsActivity)
    fun inject(activity: FingerprintSetupActivity)
    fun inject(activity: GeneralSettingsActivity)
    fun inject(activity: ManageTokensActivity)
    fun inject(activity: PairingActivity)
    fun inject(activity: PasswordConfirmActivity)
    fun inject(activity: PasswordSetupActivity)
    fun inject(activity: PasswordChangeActivity)
    fun inject(activity: PaymentTokensActivity)
    fun inject(activity: QRCodeScanActivity)
    fun inject(activity: ReceiveTokenActivity)
    fun inject(activity: RecoverSafeIntroActivity)
    fun inject(activity: RecoverSafeRecoveryPhraseActivity)
    fun inject(activity: RecoveryPhraseIntroActivity)
    fun inject(activity: ReplaceExtensionSubmitActivity)
    fun inject(activity: ReplaceExtensionRecoveryPhraseActivity)
    fun inject(activity: ReviewTransactionActivity)
    fun inject(activity: SafeMainActivity)
    fun inject(activity: ScanExtensionAddressActivity)
    fun inject(activity: SelectTokenActivity)
    fun inject(activity: SetupNewRecoveryPhraseActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TransactionStatusActivity)
    fun inject(activity: UnlockActivity)
    fun inject(activity: WalletConnectIntroActivity)
    fun inject(activity: WalletConnectLinkActivity)
    fun inject(activity: WalletConnectSessionsActivity)

    fun inject(activity: SignatureRequestActivity)

    // Dialogs

    fun inject(dialog: EnsInputDialog)
    fun inject(dialog: FingerprintDialog)
    fun inject(dialog: SimpleAddressShareDialog)
    fun inject(dialog: UnlockDialog)
}
