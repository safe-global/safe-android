package pm.gnosis.heimdall.common.di.modules

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.ui.account.AccountContract
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.authenticate.AuthenticateContract
import pm.gnosis.heimdall.ui.credits.BuyCreditsContract
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateTokenTransactionProgressContract
import pm.gnosis.heimdall.ui.extensions.recovery.RecoveryStatusContract
import pm.gnosis.heimdall.ui.onboarding.account.AccountSetupContract
import pm.gnosis.heimdall.ui.onboarding.account.create.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.account.restore.RestoreAccountContract
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.safe.add.AddSafeContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewContract
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeContract
import pm.gnosis.heimdall.ui.security.unlock.UnlockContract
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsContract
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsContract
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordContract
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicContract
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementContract
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.tokens.add.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoContract
import pm.gnosis.heimdall.ui.transactions.ReceiptTransactionContract
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract
import pm.gnosis.heimdall.ui.transactions.details.assets.AssetTransferDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.extensions.recovery.AddRecoveryExtensionContract
import pm.gnosis.heimdall.ui.transactions.details.generic.GenericTransactionDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsContract
import pm.gnosis.svalinn.common.di.ForView
import pm.gnosis.svalinn.common.di.ViewContext

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
    fun providesAddRecoveryExtensionContract(provider: ViewModelProvider) = provider[AddRecoveryExtensionContract::class.java]

    @Provides
    @ForView
    fun providesAddSafeContract(provider: ViewModelProvider) = provider[AddSafeContract::class.java]

    @Provides
    @ForView
    fun providesAddTokenContract(provider: ViewModelProvider) = provider[AddTokenContract::class.java]

    @Provides
    @ForView
    fun providesAddressBookContract(provider: ViewModelProvider) = provider[AddressBookContract::class.java]

    @Provides
    @ForView
    fun providesAssetTransferTransactionDetailsContract(provider: ViewModelProvider) = provider[AssetTransferDetailsContract::class.java]

    @Provides
    @ForView
    fun providesAuthenticateContract(provider: ViewModelProvider) = provider[AuthenticateContract::class.java]

    @Provides
    @ForView
    fun providesBaseTransactionDetailsContract(provider: ViewModelProvider) = provider[BaseTransactionDetailsContract::class.java]

    @Provides
    @ForView
    fun providesBuyCreditsContract(provider: ViewModelProvider) = provider[BuyCreditsContract::class.java]

    @Provides
    @ForView
    fun providesChangePasswordContract(provider: ViewModelProvider) = provider[ChangePasswordContract::class.java]

    @Provides
    @ForView
    fun providesChangeSafeSettingsDetailsContract(provider: ViewModelProvider) = provider[ChangeSafeSettingsDetailsContract::class.java]

    @Provides
    @ForView
    fun providesCreateTokenTransactionProgressContract(provider: ViewModelProvider) = provider[CreateTokenTransactionProgressContract::class.java]

    @Provides
    @ForView
    fun providesGenerateMnemonicContract(provider: ViewModelProvider) = provider[GenerateMnemonicContract::class.java]

    @Provides
    @ForView
    fun providesGenericTransactionDetailsContract(provider: ViewModelProvider) = provider[GenericTransactionDetailsContract::class.java]

    @Provides
    @ForView
    fun providesNetworkSettingsContract(provider: ViewModelProvider) = provider[NetworkSettingsContract::class.java]

    @Provides
    @ForView
    fun providesPasswordSetupContract(provider: ViewModelProvider) = provider[PasswordSetupContract::class.java]

    @Provides
    @ForView
    fun providesRecoveryStatusContract(provider: ViewModelProvider) = provider[RecoveryStatusContract::class.java]

    @Provides
    @ForView
    fun providesRestoreAccountContract(provider: ViewModelProvider) = provider[RestoreAccountContract::class.java]

    @Provides
    @ForView
    fun providesReceiptTransactionContract(provider: ViewModelProvider) = provider[ReceiptTransactionContract::class.java]

    @Provides
    @ForView
    fun providesAccountSetupContract(provider: ViewModelProvider) = provider[AccountSetupContract::class.java]

    @Provides
    @ForView
    fun providesRevealMnemonicContract(provider: ViewModelProvider) = provider[RevealMnemonicContract::class.java]

    @Provides
    @ForView
    fun providesSafeDetailsContract(provider: ViewModelProvider) = provider[SafeDetailsContract::class.java]

    @Provides
    @ForView
    fun providesSafeInfoContract(provider: ViewModelProvider) = provider[SafeSettingsContract::class.java]

    @Provides
    @ForView
    fun providesSafeMainContract(provider: ViewModelProvider) = provider[SafeMainContract::class.java]

    @Provides
    @ForView
    fun providesSafeOverviewContract(provider: ViewModelProvider) = provider[SafeOverviewContract::class.java]

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
    fun providesTokenInfoContract(provider: ViewModelProvider) = provider[TokenInfoContract::class.java]

    @Provides
    @ForView
    fun providesTokenManagementContract(provider: ViewModelProvider) = provider[TokenManagementContract::class.java]

    @Provides
    @ForView
    fun providesUnlockContract(provider: ViewModelProvider) = provider[UnlockContract::class.java]

    @Provides
    @ForView
    fun providesViewTransactionContract(provider: ViewModelProvider) = provider[ViewTransactionContract::class.java]

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
