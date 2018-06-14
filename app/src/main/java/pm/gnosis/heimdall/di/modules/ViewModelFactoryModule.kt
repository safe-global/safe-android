package pm.gnosis.heimdall.di.modules

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import pm.gnosis.heimdall.di.ViewModelFactory
import pm.gnosis.heimdall.di.ViewModelKey
import pm.gnosis.heimdall.ui.account.AccountContract
import pm.gnosis.heimdall.ui.account.AccountViewModel
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.addressbook.AddressBookViewModel
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsContract
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsViewModel
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupContract
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupViewModel
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupViewModel
import pm.gnosis.heimdall.ui.safe.create.*
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsViewModel
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsViewModel
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainViewModel
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressContract
import pm.gnosis.heimdall.ui.safe.pending.DeploySafeProgressViewModel
import pm.gnosis.heimdall.ui.safe.pending.PendingSafeContract
import pm.gnosis.heimdall.ui.safe.pending.PendingSafeViewModel
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeContract
import pm.gnosis.heimdall.ui.safe.selection.SelectSafeViewModel
import pm.gnosis.heimdall.ui.security.unlock.UnlockContract
import pm.gnosis.heimdall.ui.security.unlock.UnlockViewModel
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsContract
import pm.gnosis.heimdall.ui.settings.network.NetworkSettingsViewModel
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsContract
import pm.gnosis.heimdall.ui.settings.security.SecuritySettingsViewModel
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordContract
import pm.gnosis.heimdall.ui.settings.security.changepassword.ChangePasswordViewModel
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.SplashViewModel
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesViewModel
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensContract
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensViewModel
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenContract
import pm.gnosis.heimdall.ui.tokens.receive.ReceiveTokenViewModel
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferContract
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferViewModel
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionContract
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusContract
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {
    @Binds
    @IntoMap
    @ViewModelKey(AccountContract::class)
    abstract fun bindsAccountContract(viewModel: AccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddressBookContract::class)
    abstract fun bindsAddressBookContract(viewModel: AddressBookViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmSafeRecoveryPhraseContract::class)
    abstract fun bindsConfirmSafeRecoveryPhraseContract(viewModel: ConfirmSafeRecoveryPhraseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChangePasswordContract::class)
    abstract fun bindsChangePasswordContract(viewModel: ChangePasswordViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmTransactionContract::class)
    abstract fun bindsConfirmTransactionContract(viewModel: ConfirmTransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CreateAssetTransferContract::class)
    abstract fun bindsCreateAssetTransferContract(viewModel: CreateAssetTransferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DebugSettingsContract::class)
    abstract fun bindsDebugSettingsContract(viewModel: DebugSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeploySafeProgressContract::class)
    abstract fun bindsDeploySafeProgressContract(viewModel: DeploySafeProgressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FingerprintSetupContract::class)
    abstract fun bindsFingerprintSetupContract(viewModel: FingerprintSetupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ManageTokensContract::class)
    abstract fun bindsManageTokensContract(viewModel: ManageTokensViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkSettingsContract::class)
    abstract fun bindsNetworkSettingsContract(viewModel: NetworkSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PairingContract::class)
    abstract fun bindsPairingContract(viewModel: PairingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PasswordSetupContract::class)
    abstract fun bindsPasswordSetupContract(viewModel: PasswordSetupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PendingSafeContract::class)
    abstract fun bindsPendingSafeContract(viewModel: PendingSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReceiveTokenContract::class)
    abstract fun bindsReceiveTokenContract(viewModel: ReceiveTokenViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReviewTransactionContract::class)
    abstract fun bindsReviewTransactionContract(viewModel: ReviewTransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeDetailsContract::class)
    abstract fun bindsSafeDetailsContract(viewModel: SafeDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeMainContract::class)
    abstract fun bindsSafeMainContract(viewModel: SafeMainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeRecoveryPhraseContract::class)
    abstract fun bindsSafeRecoveryPhraseContract(viewModel: SafeRecoveryPhraseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeTransactionsContract::class)
    abstract fun bindsSafeTransactionsContract(viewModel: SafeTransactionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SecuritySettingsContract::class)
    abstract fun bindsSecuritySettingsContract(viewModel: SecuritySettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectSafeContract::class)
    abstract fun bindsSelectSafeContract(viewModel: SelectSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashContract::class)
    abstract fun bindsSplashContract(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokenBalancesContract::class)
    abstract fun bindsTokensContract(viewModel: TokenBalancesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionStatusContract::class)
    abstract fun bindsTransactionStatusContract(viewModel: TransactionStatusViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UnlockContract::class)
    abstract fun bindsUnlockContract(viewModel: UnlockViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
