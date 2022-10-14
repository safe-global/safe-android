package io.gnosis.safe.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.gnosis.safe.di.ViewModelFactory
import io.gnosis.safe.di.ViewModelKey
import io.gnosis.safe.ui.assets.AssetsViewModel
import io.gnosis.safe.ui.assets.coins.CoinsViewModel
import io.gnosis.safe.ui.assets.collectibles.CollectiblesViewModel
import io.gnosis.safe.ui.dialogs.EnsInputViewModel
import io.gnosis.safe.ui.dialogs.UnstoppableInputViewModel
import io.gnosis.safe.ui.safe.add.AddSafeNameViewModel
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.gnosis.safe.ui.safe.send_funds.AssetSelectionViewModel
import io.gnosis.safe.ui.safe.send_funds.SendAssetViewModel
import io.gnosis.safe.ui.safe.share.ShareSafeViewModel
import io.gnosis.safe.ui.settings.SettingsViewModel
import io.gnosis.safe.ui.settings.app.AppSettingsViewModel
import io.gnosis.safe.ui.settings.app.GetInTouchViewModel
import io.gnosis.safe.ui.settings.app.fiat.AppFiatViewModel
import io.gnosis.safe.ui.settings.chain.ChainSelectionViewModel
import io.gnosis.safe.ui.settings.owner.OwnerEditNameViewModel
import io.gnosis.safe.ui.settings.owner.OwnerEnterNameViewModel
import io.gnosis.safe.ui.settings.owner.OwnerSeedPhraseViewModel
import io.gnosis.safe.ui.settings.owner.details.OwnerDetailsViewModel
import io.gnosis.safe.ui.settings.owner.intro.OwnerGenerateViewModel
import io.gnosis.safe.ui.settings.owner.ledger.LedgerDeviceListViewModel
import io.gnosis.safe.ui.settings.owner.ledger.LedgerOwnerSelectionViewModel
import io.gnosis.safe.ui.settings.owner.ledger.LedgerSignViewModel
import io.gnosis.safe.ui.settings.owner.list.OwnerListViewModel
import io.gnosis.safe.ui.settings.owner.selection.OwnerSelectionViewModel
import io.gnosis.safe.ui.settings.safe.AdvancedSafeSettingsViewModel
import io.gnosis.safe.ui.settings.safe.SafeSettingsEditNameViewModel
import io.gnosis.safe.ui.settings.safe.SafeSettingsViewModel
import io.gnosis.safe.ui.splash.SplashViewModel
import io.gnosis.safe.ui.transactions.TransactionListViewModel
import io.gnosis.safe.ui.transactions.TransactionsViewModel
import io.gnosis.safe.ui.transactions.details.ConfirmRejectionViewModel
import io.gnosis.safe.ui.transactions.details.TransactionDetailsActionViewModel
import io.gnosis.safe.ui.transactions.details.TransactionDetailsViewModel
import io.gnosis.safe.ui.updates.UpdatesViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChainSelectionViewModel::class)
    abstract fun bindsChainSelectionViewModel(viewModel: ChainSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeViewModel::class)
    abstract fun bindsAddSafeViewModel(viewModel: AddSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EnsInputViewModel::class)
    abstract fun providesEnsInputViewModel(viewModel: EnsInputViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UnstoppableInputViewModel::class)
    abstract fun providesUnstoppableInputViewModel(viewModel: UnstoppableInputViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeNameViewModel::class)
    abstract fun providesAddSafeNameViewModel(viewModel: AddSafeNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CoinsViewModel::class)
    abstract fun providesCoinsViewModel(viewModel: CoinsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CollectiblesViewModel::class)
    abstract fun providesCollectiblesViewModel(viewModel: CollectiblesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssetsViewModel::class)
    abstract fun providesAssetsViewModel(viewModel: AssetsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssetSelectionViewModel::class)
    abstract fun providesAssetSelectionViewModel(viewModel: AssetSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SendAssetViewModel::class)
    abstract fun providesSendAssetViewModel(viewModel: SendAssetViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSelectionViewModel::class)
    abstract fun providesSafeSelectionViewModel(viewModel: SafeSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerSelectionViewModel::class)
    abstract fun providesOwnerSelectionViewModel(viewModel: OwnerSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LedgerOwnerSelectionViewModel::class)
    abstract fun providesLedgerOwnerSelectionViewModel(viewModel: LedgerOwnerSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerGenerateViewModel::class)
    abstract fun providesOwnerGenerateViewModel(viewModel: OwnerGenerateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerEnterNameViewModel::class)
    abstract fun providesOwnerEnterNameViewModel(viewModel: OwnerEnterNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerEditNameViewModel::class)
    abstract fun providesOwnerEditNameViewModel(viewModel: OwnerEditNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerListViewModel::class)
    abstract fun providesOwnerListViewModel(viewModel: OwnerListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerDetailsViewModel::class)
    abstract fun providesOwnerDetailsViewModel(viewModel: OwnerDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun providesSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsViewModel::class)
    abstract fun providesSafeSettingsViewModel(viewModel: SafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsEditNameViewModel::class)
    abstract fun providesSafeSettingsEditNameViewModel(viewModel: SafeSettingsEditNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AppSettingsViewModel::class)
    abstract fun providesAppSettingsViewModel(viewModel: AppSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionsViewModel::class)
    abstract fun providesTransactionsViewModel(viewModel: TransactionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionListViewModel::class)
    abstract fun providesTransactionListViewModel(viewModel: TransactionListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailsViewModel::class)
    abstract fun providesTransactionDetailsViewModel(viewModel: TransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailsActionViewModel::class)
    abstract fun providesTransactionDetailsActionViewModel(viewModel: TransactionDetailsActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfirmRejectionViewModel::class)
    abstract fun providesConfirmRejectionViewModel(viewModel: ConfirmRejectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdvancedSafeSettingsViewModel::class)
    abstract fun providesAdvancedSafeSettingsViewModel(viewModel: AdvancedSafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ShareSafeViewModel::class)
    abstract fun providesSharedSafeViewModel(viewModel: ShareSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GetInTouchViewModel::class)
    abstract fun providesGetInTouchViewModel(viewModel: GetInTouchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerSeedPhraseViewModel::class)
    abstract fun providesOwnerSeedPhraseViewModel(viewModel: OwnerSeedPhraseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AppFiatViewModel::class)
    abstract fun providesAppFiatViewModel(viewModel: AppFiatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UpdatesViewModel::class)
    abstract fun providesUpdatesViewModel(viewModel: UpdatesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LedgerDeviceListViewModel::class)
    abstract fun providesLedgerDeviceListViewModel(viewModel: LedgerDeviceListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LedgerSignViewModel::class)
    abstract fun providesLedgerSignViewModel(viewModel: LedgerSignViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
