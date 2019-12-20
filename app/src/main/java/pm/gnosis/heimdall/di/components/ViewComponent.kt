package pm.gnosis.heimdall.di.components

import dagger.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.addressbook.edit.AddressBookEditEntryActivity
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.two_factor.Connect2FaActivity
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsActivity
import pm.gnosis.heimdall.ui.deeplinks.DeeplinkActivity
import pm.gnosis.heimdall.ui.dialogs.ens.EnsInputDialog
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.ui.two_factor.keycard.*
import pm.gnosis.heimdall.ui.messagesigning.ConfirmMessageActivity
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordConfirmActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.heimdall.ui.safe.pairing.remove.Remove2FaRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeConfirmRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeStepsActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafePaymentTokenActivity
import pm.gnosis.heimdall.ui.safe.create.CreateSafeSetupRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsFragment
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.safe.main.NoSafesFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.pairing.PairingStartActivity
import pm.gnosis.heimdall.ui.two_factor.authenticator.PairingAuthenticatorActivity
import pm.gnosis.heimdall.ui.safe.pairing.PairingSubmitActivity
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressFragment
import pm.gnosis.heimdall.ui.safe.pending.SafeCreationFundFragment
import pm.gnosis.heimdall.ui.safe.pairing.replace.Replace2FaRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ConfirmNewRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ScanExtensionAddressActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.SetupNewRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.SetupNewRecoveryPhraseIntroActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeIntroActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeRecoveryPhraseActivity
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeFundFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafePendingFragment
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeSubmitFragment
import pm.gnosis.heimdall.ui.safe.upgrade.UpgradeMasterCopyActivity
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
import pm.gnosis.heimdall.ui.transactions.view.details.MultiSendDetailsActivity
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

    fun inject(fragment: ChangePasswordEnterOldFragment)
    fun inject(fragment: ChangePasswordEnterNewFragment)
    fun inject(fragment: DeploySafeProgressFragment)
    fun inject(fragment: KeycardInitializeReadingCardFragment)
    fun inject(fragment: KeycardPairingInputFragment)
    fun inject(fragment: KeycardPairingReadingCardFragment)
    fun inject(fragment: KeycardSigningReadingCardFragment)
    fun inject(fragment: KeycardSigningInputFragment)

    fun inject(fragment: KeycardNoSlotsFragment)
    fun inject(fragment: KeycardBlockedFragment)

    fun inject(fragment: NoSafesFragment)
    fun inject(fragment: RecoveringSafeFragment)
    fun inject(fragment: RecoveringSafeFundFragment)
    fun inject(fragment: RecoveringSafePendingFragment)
    fun inject(fragment: RecoveringSafeSubmitFragment)
    fun inject(fragment: SafeCreationFundFragment)
    fun inject(fragment: SafeDetailsFragment)
    fun inject(fragment: SafeTransactionsFragment)
    fun inject(fragment: TokenBalancesFragment)

    // Activities

    fun inject(activity: AddressBookActivity)
    fun inject(activity: AddressBookAddEntryActivity)
    fun inject(activity: AddressBookEditEntryActivity)
    fun inject(activity: AddressBookEntryDetailsActivity)
    fun inject(activity: CheckSafeActivity)
    fun inject(activity: ConfirmTransactionActivity)
    fun inject(activity: ConfirmMessageActivity)
    fun inject(activity: ConfirmNewRecoveryPhraseActivity)
    fun inject(activity: Connect2FaActivity)
    fun inject(activity: CreateAssetTransferActivity)
    fun inject(activity: CreateSafeStepsActivity)
    fun inject(activity: CreateSafeConfirmRecoveryPhraseActivity)
    fun inject(activity: CreateSafePaymentTokenActivity)
    fun inject(activity: CreateSafeSetupRecoveryPhraseActivity)
    fun inject(activity: DebugSettingsActivity)
    fun inject(activity: DeeplinkActivity)
    fun inject(activity: FingerprintSetupActivity)
    fun inject(activity: GeneralSettingsActivity)
    fun inject(activity: KeycardInitializeActivity)
    fun inject(activity: ManageTokensActivity)
    fun inject(activity: MultiSendDetailsActivity)
    fun inject(activity: PairingAuthenticatorActivity)
    fun inject(activity: PasswordConfirmActivity)
    fun inject(activity: PasswordSetupActivity)
    fun inject(activity: PasswordChangeActivity)
    fun inject(activity: PaymentTokensActivity)
    fun inject(activity: QRCodeScanActivity)
    fun inject(activity: ReceiveTokenActivity)
    fun inject(activity: RecoverSafeIntroActivity)
    fun inject(activity: RecoverSafeRecoveryPhraseActivity)
    fun inject(activity: RecoveryPhraseIntroActivity)
    fun inject(activity: Replace2FaRecoveryPhraseActivity)
    fun inject(activity: Remove2FaRecoveryPhraseActivity)
    fun inject(activity: ReviewTransactionActivity)
    fun inject(activity: SafeMainActivity)
    fun inject(activity: ScanExtensionAddressActivity)
    fun inject(activity: SelectTokenActivity)
    fun inject(activity: SetupNewRecoveryPhraseActivity)
    fun inject(activity: SetupNewRecoveryPhraseIntroActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TransactionStatusActivity)
    fun inject(activity: UnlockActivity)
    fun inject(activity: UpgradeMasterCopyActivity)
    fun inject(activity: WalletConnectIntroActivity)
    fun inject(activity: WalletConnectLinkActivity)
    fun inject(activity: WalletConnectSessionsActivity)
    fun inject(activity: PairingStartActivity)
    fun inject(activity: PairingSubmitActivity)

    // Dialogs

    fun inject(dialog: EnsInputDialog)
    fun inject(dialog: FingerprintDialog)
    fun inject(dialog: KeycardInitializeDialog)
    fun inject(dialog: KeycardPairingDialog)
    fun inject(dialog: KeycardSigningDialog)
    fun inject(dialog: SimpleAddressShareDialog)
    fun inject(dialog: UnlockDialog)
}
