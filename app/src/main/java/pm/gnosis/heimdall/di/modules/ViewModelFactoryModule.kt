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
import pm.gnosis.heimdall.ui.authenticate.AuthenticateContract
import pm.gnosis.heimdall.ui.authenticate.AuthenticateViewModel
import pm.gnosis.heimdall.ui.credits.BuyCreditsContract
import pm.gnosis.heimdall.ui.credits.BuyCreditsViewModel
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsContract
import pm.gnosis.heimdall.ui.debugsettings.DebugSettingsViewModel
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateTokenTransactionProgressContract
import pm.gnosis.heimdall.ui.dialogs.transaction.CreateTokenTransactionProgressViewModel
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupContract
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupViewModel
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupViewModel
import pm.gnosis.heimdall.ui.safe.add.AddSafeContract
import pm.gnosis.heimdall.ui.safe.add.AddSafeViewModel
import pm.gnosis.heimdall.ui.safe.create.PairingContract
import pm.gnosis.heimdall.ui.safe.create.PairingViewModel
import pm.gnosis.heimdall.ui.safe.create.SafeRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.create.SafeRecoveryPhraseViewModel
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsViewModel
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsContract
import pm.gnosis.heimdall.ui.safe.details.info.SafeSettingsViewModel
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsViewModel
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainViewModel
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewContract
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
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
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicContract
import pm.gnosis.heimdall.ui.settings.security.revealmnemonic.RevealMnemonicViewModel
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementContract
import pm.gnosis.heimdall.ui.settings.tokens.TokenManagementViewModel
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.SplashViewModel
import pm.gnosis.heimdall.ui.tokens.add.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.add.AddTokenViewModel
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesContract
import pm.gnosis.heimdall.ui.tokens.balances.TokenBalancesViewModel
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoContract
import pm.gnosis.heimdall.ui.tokens.info.TokenInfoViewModel
import pm.gnosis.heimdall.ui.transactions.ReceiptTransactionContract
import pm.gnosis.heimdall.ui.transactions.ReceiptTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.ViewTransactionContract
import pm.gnosis.heimdall.ui.transactions.ViewTransactionViewModel
import pm.gnosis.heimdall.ui.transactions.details.assets.AssetTransferDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.assets.AssetTransferDetailsViewModel
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.base.BaseTransactionDetailsViewModel
import pm.gnosis.heimdall.ui.transactions.details.extensions.recovery.AddRecoveryExtensionContract
import pm.gnosis.heimdall.ui.transactions.details.extensions.recovery.AddRecoveryExtensionViewModel
import pm.gnosis.heimdall.ui.transactions.details.generic.GenericTransactionDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.generic.GenericTransactionDetailsViewModel
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsContract
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsViewModel
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
    @ViewModelKey(AddRecoveryExtensionContract::class)
    abstract fun bindsAddRecoveryExtensionContract(viewModel: AddRecoveryExtensionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeContract::class)
    abstract fun bindsAddSafeContract(viewModel: AddSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddTokenContract::class)
    abstract fun bindsAddTokenContract(viewModel: AddTokenViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssetTransferDetailsContract::class)
    abstract fun bindsAssetTransferTransactionDetailsContract(viewModel: AssetTransferDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AuthenticateContract::class)
    abstract fun bindsAuthenticateContract(viewModel: AuthenticateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseTransactionDetailsContract::class)
    abstract fun bindsBaseTransactionDetailsContract(viewModel: BaseTransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BuyCreditsContract::class)
    abstract fun bindsBuyCreditsContract(viewModel: BuyCreditsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChangePasswordContract::class)
    abstract fun bindsChangePasswordContract(viewModel: ChangePasswordViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChangeSafeSettingsDetailsContract::class)
    abstract fun bindsChangeSafeSettingsDetailsContract(viewModel: ChangeSafeSettingsDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CreateTokenTransactionProgressContract::class)
    abstract fun bindsCreateTokenTransactionProgressContract(viewModel: CreateTokenTransactionProgressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DebugSettingsContract::class)
    abstract fun bindsDebugSettingsContract(viewModel: DebugSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FingerprintSetupContract::class)
    abstract fun bindsFingerprintSetupContract(viewModel: FingerprintSetupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GenericTransactionDetailsContract::class)
    abstract fun bindsGenericTransactionDetailsContract(viewModel: GenericTransactionDetailsViewModel): ViewModel

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
    @ViewModelKey(ReceiptTransactionContract::class)
    abstract fun bindsReceiptTransactionContract(viewModel: ReceiptTransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RevealMnemonicContract::class)
    abstract fun bindsRevealMnemonicContract(viewModel: RevealMnemonicViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeDetailsContract::class)
    abstract fun bindsSafeDetailsContract(viewModel: SafeDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsContract::class)
    abstract fun bindsSafeInfoContract(viewModel: SafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeMainContract::class)
    abstract fun bindsSafeMainContract(viewModel: SafeMainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeOverviewContract::class)
    abstract fun bindsSafeOverviewContract(viewModel: SafeOverviewViewModel): ViewModel

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
    @ViewModelKey(TokenInfoContract::class)
    abstract fun bindsTokensInfoContract(viewModel: TokenInfoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TokenManagementContract::class)
    abstract fun bindsTokenManagementContract(viewModel: TokenManagementViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ViewTransactionContract::class)
    abstract fun bindsViewTransactionContract(viewModel: ViewTransactionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UnlockContract::class)
    abstract fun bindsUnlockContract(viewModel: UnlockViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
