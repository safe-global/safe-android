package pm.gnosis.heimdall.di.modules

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.account.AccountContract
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsContract
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupContract
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.safe.create.ConfirmSafeRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.create.PairingContract
import pm.gnosis.heimdall.ui.safe.create.SafeRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressContract
import pm.gnosis.heimdall.ui.safe.pending.PendingSafeContract
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeContract
import pm.gnosis.heimdall.ui.security.unlock.UnlockContract
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsContract
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordContract
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensContract
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenContract
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusContract

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
    fun providesAccountContract(provider: ViewModelProvider) = provider[AccountContract::class.java]

    @Provides
    @ForView
    fun providesAddressBookContract(provider: ViewModelProvider) = provider[AddressBookContract::class.java]

    @Provides
    @ForView
    fun providesConfirmSafeRecoveryPhraseContract(provider: ViewModelProvider) = provider[ConfirmSafeRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesChangePasswordContract(provider: ViewModelProvider) = provider[ChangePasswordContract::class.java]

    @Provides
    @ForView
    fun providesConfirmTransactionContract(provider: ViewModelProvider) = provider[ConfirmTransactionContract::class.java]

    @Provides
    @ForView
    fun providesCreateAssetTransferContract(provider: ViewModelProvider) = provider[CreateAssetTransferContract::class.java]

    @Provides
    @ForView
    fun providesDebugSettingsContract(provider: ViewModelProvider) = provider[DebugSettingsContract::class.java]

    @Provides
    @ForView
    fun providesDeploySafeProgressContract(provider: ViewModelProvider) = provider[DeploySafeProgressContract::class.java]

    @Provides
    @ForView
    fun providesFingerprintSetupContract(provider: ViewModelProvider) = provider[FingerprintSetupContract::class.java]

    @Provides
    @ForView
    fun providesManageTokensContract(provider: ViewModelProvider) = provider[ManageTokensContract::class.java]

    @Provides
    @ForView
    fun providesPairingContract(provider: ViewModelProvider) = provider[PairingContract::class.java]

    @Provides
    @ForView
    fun providesPasswordSetupContract(provider: ViewModelProvider) = provider[PasswordSetupContract::class.java]

    @Provides
    @ForView
    fun providesPendingSafeContract(provider: ViewModelProvider) = provider[PendingSafeContract::class.java]

    @Provides
    @ForView
    fun providesReceiveTokenContract(provider: ViewModelProvider) = provider[ReceiveTokenContract::class.java]

    @Provides
    @ForView
    fun providesReviewTransactionContract(provider: ViewModelProvider) = provider[ReviewTransactionContract::class.java]

    @Provides
    @ForView
    fun providesSafeDetailsContract(provider: ViewModelProvider) = provider[SafeDetailsContract::class.java]

    @Provides
    @ForView
    fun providesSafeMainContract(provider: ViewModelProvider) = provider[SafeMainContract::class.java]

    @Provides
    @ForView
    fun providesSafeRecoveryPhraseContract(provider: ViewModelProvider) = provider[SafeRecoveryPhraseContract::class.java]

    @Provides
    @ForView
    fun providesSafeTransactionsContract(provider: ViewModelProvider) = provider[SafeTransactionsContract::class.java]

    @Provides
    @ForView
    fun providesSecuritySettingsContract(provider: ViewModelProvider) = provider[SecuritySettingsContract::class.java]

    @Provides
    @ForView
    fun providesSelectSafeContract(provider: ViewModelProvider) = provider[SelectSafeContract::class.java]

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
    fun providesViewModelProvider(factory: ViewModelProvider.Factory): ViewModelProvider {
        return when (context) {
            is Fragment -> ViewModelProviders.of(context, factory)
            is FragmentActivity -> ViewModelProviders.of(context, factory)
            else -> throw IllegalArgumentException("Unsupported context $context")
        }
    }
}
