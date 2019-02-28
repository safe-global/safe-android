package pm.gnosis.heimdall.di.modules

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsContract
import pm.gnosis.heimdall.ui.dialogs.ens.EnsInputContract
import pm.gnosis.heimdall.ui.messagesigning.CollectMessageSignaturesContract
import pm.gnosis.heimdall.ui.messagesigning.ConfirmMessageContract
import pm.gnosis.heimdall.ui.messagesigning.ReviewPayloadContract
import pm.gnosis.heimdall.ui.messagesigning.SignatureRequestContract
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupContract
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.recoveryphrase.ConfirmRecoveryPhraseContract
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.create.CreateSafeConfirmRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.create.CreateSafePaymentTokenContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.safe.pairing.PairingContract
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressContract
import pm.gnosis.heimdall.ui.safe.pending.SafeCreationFundContract
import pm.gnosis.heimdall.ui.safe.recover.extension.ReplaceExtensionRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.recover.extension.ReplaceExtensionSubmitContract
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ConfirmNewRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.recover.recoveryphrase.ScanExtensionAddressContract
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeContract
import pm.gnosis.heimdall.ui.safe.recover.safe.RecoverSafeRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.recover.safe.submit.RecoveringSafeContract
import pm.gnosis.heimdall.ui.security.unlock.UnlockContract
import pm.gnosis.heimdall.ui.settings.general.GeneralSettingsContract
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordContract
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensContract
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensContract
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenContract
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusContract
import pm.gnosis.heimdall.ui.walletconnect.intro.WalletConnectIntroContract
import pm.gnosis.heimdall.ui.walletconnect.link.WalletConnectLinkContract
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsContract

@Module
class ViewModule(val context: Context) {
    @Provides
    @ForView
    @ViewContext
    fun providesContext() = context

    @Provides
    @ForView
    fun providesLinearLayoutManager() = LinearLayoutManager(context)

    @Provides
    @ForView
    fun providesAddressBookContract(provider: ViewModelProvider) = provider[AddressBookContract::class.java]

    @Provides
    @ForView
    fun providesChangePasswordContract(provider: ViewModelProvider) = provider[ChangePasswordContract::class.java]

    @Provides
    @ForView
    fun providesCheckSafeContract(provider: ViewModelProvider) = provider[CheckSafeContract::class.java]

    @Provides
    @ForView
    fun providesCollectMessageSignaturesContract(provider: ViewModelProvider) = provider[CollectMessageSignaturesContract::class.java]

    @Provides
    @ForView
    fun providesConfirmMessageContract(provider: ViewModelProvider) = provider[ConfirmMessageContract::class.java]

    @Provides
    @ForView
    fun providesSignatureRequestContract(provider: ViewModelProvider) = provider[SignatureRequestContract::class.java]

    @Provides
    @ForView
    fun providesConfirmSafeRecoveryPhraseContract(provider: ViewModelProvider) = provider[ConfirmRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesConfirmTransactionContract(provider: ViewModelProvider) = provider[ConfirmTransactionContract::class.java]

    @Provides
    @ForView
    fun providesCreateAssetTransferContract(provider: ViewModelProvider) = provider[CreateAssetTransferContract::class.java]

    @Provides
    @ForView
    fun providesCreateSafeConfirmSetupRecoveryPhraseContract(provider: ViewModelProvider) =
        provider[CreateSafeConfirmRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesCreateSafePaymentTokenContract(provider: ViewModelProvider) = provider[CreateSafePaymentTokenContract::class.java]

    @Provides
    @ForView
    fun providesDebugSettingsContract(provider: ViewModelProvider) = provider[DebugSettingsContract::class.java]

    @Provides
    @ForView
    fun providesDeploySafeProgressContract(provider: ViewModelProvider) = provider[DeploySafeProgressContract::class.java]

    @Provides
    @ForView
    fun providesEnsInputContract(provider: ViewModelProvider) = provider[EnsInputContract::class.java]

    @Provides
    @ForView
    fun providesFingerprintSetupContract(provider: ViewModelProvider) = provider[FingerprintSetupContract::class.java]

    @Provides
    @ForView
    fun providesGeneralSettingsContract(provider: ViewModelProvider) = provider[GeneralSettingsContract::class.java]

    @Provides
    @ForView
    fun providesManageTokensContract(provider: ViewModelProvider) = provider[ManageTokensContract::class.java]

    @Provides
    @ForView
    fun providesNewConfirmRecoveryPhraseContract(provider: ViewModelProvider) = provider[ConfirmNewRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesPairingContract(provider: ViewModelProvider) = provider[PairingContract::class.java]

    @Provides
    @ForView
    fun providesPasswordSetupContract(provider: ViewModelProvider) = provider[PasswordSetupContract::class.java]

    @Provides
    @ForView
    fun providesPaymentTokensContract(provider: ViewModelProvider) = provider[PaymentTokensContract::class.java]

    @Provides
    @ForView
    fun providesReceiveTokenContract(provider: ViewModelProvider) = provider[ReceiveTokenContract::class.java]

    @Provides
    @ForView
    fun providesReplaceExtensionContract(provider: ViewModelProvider) = provider[ReplaceExtensionSubmitContract::class.java]

    @Provides
    @ForView
    fun providesReplaceExtensionRecoveryPhraseContract(provider: ViewModelProvider) =
        provider[ReplaceExtensionRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesRecoveringSafeContract(provider: ViewModelProvider) = provider[RecoveringSafeContract::class.java]

    @Provides
    @ForView
    fun providesRecoverSafeRecoveryPhraseContract(provider: ViewModelProvider) = provider[RecoverSafeRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesReviewPayloadContract(provider: ViewModelProvider) = provider[ReviewPayloadContract::class.java]

    @Provides
    @ForView
    fun providesReviewTransactionContract(provider: ViewModelProvider) = provider[ReviewTransactionContract::class.java]

    @Provides
    @ForView
    fun providesSafeCreationFundContract(provider: ViewModelProvider) = provider[SafeCreationFundContract::class.java]

    @Provides
    @ForView
    fun providesSafeDetailsContract(provider: ViewModelProvider) = provider[SafeDetailsContract::class.java]

    @Provides
    @ForView
    fun providesSafeMainContract(provider: ViewModelProvider) = provider[SafeMainContract::class.java]

    @Provides
    @ForView
    fun providesSafeTransactionsContract(provider: ViewModelProvider) = provider[SafeTransactionsContract::class.java]

    @Provides
    @ForView
    fun providesScanExtensionAddressContract(provider: ViewModelProvider) = provider[ScanExtensionAddressContract::class.java]

    @Provides
    @ForView
    fun providesSetupRecoveryPhraseContract(provider: ViewModelProvider) = provider[SetupRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesSplashContract(provider: ViewModelProvider) = provider[SplashContract::class.java]

    @Provides
    @ForView
    fun providesTokenBalancesContract(provider: ViewModelProvider) = provider[TokenBalancesContract::class.java]

    @Provides
    @ForView
    fun providesTransactionStatusContract(provider: ViewModelProvider) = provider[TransactionStatusContract::class.java]

    @Provides
    @ForView
    fun providesUnlockContract(provider: ViewModelProvider) = provider[UnlockContract::class.java]

    @Provides
    @ForView
    fun providesWalletConnectIntroContract(provider: ViewModelProvider) = provider[WalletConnectIntroContract::class.java]

    @Provides
    @ForView
    fun providesWalletConnectLinkContract(provider: ViewModelProvider) = provider[WalletConnectLinkContract::class.java]

    @Provides
    @ForView
    fun providesWalletConnectSessionsContract(provider: ViewModelProvider) = provider[WalletConnectSessionsContract::class.java]

    @Provides
    @ForView
    fun providesViewModelProvider(factory: ViewModelProvider.Factory): ViewModelProvider {
        return when (context) {
            is Fragment -> ViewModelProviders.of(context, factory)
            is FragmentActivity -> ViewModelProviders.of(context, factory)
            else -> throw IllegalArgumentException("Unsupported context $context")
        }
    }
}
