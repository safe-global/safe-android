package io.gnosis.safe.di.modules

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.ViewContext
import io.gnosis.safe.ui.assets.AssetsViewModel
import io.gnosis.safe.ui.assets.coins.CoinsAdapter
import io.gnosis.safe.ui.assets.coins.CoinsViewModel
import io.gnosis.safe.ui.assets.collectibles.CollectiblesViewModel
import io.gnosis.safe.ui.dialogs.EnsInputViewModel
import io.gnosis.safe.ui.dialogs.UnstoppableInputViewModel
import io.gnosis.safe.ui.safe.add.AddSafeNameViewModel
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionAdapter
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.gnosis.safe.ui.safe.send_funds.AssetSelectionViewModel
import io.gnosis.safe.ui.safe.send_funds.SendAssetReviewViewModel
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
import io.gnosis.safe.ui.settings.owner.selection.KeystoneOwnerSelectionViewModel
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
import io.gnosis.safe.ui.transactions.execution.TxEditFeeViewModel
import io.gnosis.safe.ui.updates.UpdatesViewModel
import java.lang.ref.WeakReference
import javax.inject.Named

@Module
class ViewModule(
    private val context: Context,
    private val viewModelProvider: Any? = null
) {

    @Provides
    @ForView
    @ViewContext
    fun providesContext() = context

    @Provides
    @ForView
    fun providesLinearLayoutManager() = LinearLayoutManager(context)

    @Provides
    @ForView
    fun providesViewModelProvider(factory: ViewModelProvider.Factory): ViewModelProvider {
        return when (val provider = viewModelProvider ?: context) {
            is Fragment -> ViewModelProvider(provider, factory)
            is FragmentActivity -> ViewModelProvider(provider, factory)
            else -> throw IllegalArgumentException("Unsupported context $provider")
        }
    }

    @Provides
    @ForView
    fun providesSafeSelectionAdapter(safeSelectionViewModel: SafeSelectionViewModel) =
        SafeSelectionAdapter(WeakReference(safeSelectionViewModel))

    @Provides
    @ForView
    @Named("coins")
    fun providesCoinsAdapter(coinsViewModel: CoinsViewModel) =
        CoinsAdapter(WeakReference(coinsViewModel), null)

    @Provides
    @ForView
    @Named("assetSelection")
    fun providesAssetSelectionCoinsAdapter(assetSelectionViewModel: AssetSelectionViewModel) =
        CoinsAdapter(null, WeakReference(assetSelectionViewModel))

    @Provides
    @ForView
    fun providesChainSelectionViewModel(provider: ViewModelProvider) = provider[ChainSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesAddSafeViewModel(provider: ViewModelProvider) = provider[AddSafeViewModel::class.java]

    @Provides
    @ForView
    fun providesSplashViewModel(provider: ViewModelProvider) = provider[SplashViewModel::class.java]

    @Provides
    @ForView
    fun providesEnsInputViewModel(provider: ViewModelProvider) = provider[EnsInputViewModel::class.java]

    @Provides
    @ForView
    fun providesUnstoppableInputViewModel(provider: ViewModelProvider) = provider[UnstoppableInputViewModel::class.java]

    @Provides
    @ForView
    fun providesAddSafeNameViewModel(provider: ViewModelProvider) = provider[AddSafeNameViewModel::class.java]

    @Provides
    @ForView
    fun providesCoinsViewModel(provider: ViewModelProvider) = provider[CoinsViewModel::class.java]

    @Provides
    @ForView
    fun providesCollectiblesViewModel(provider: ViewModelProvider) = provider[CollectiblesViewModel::class.java]

    @Provides
    @ForView
    fun providesAssetsViewModel(provider: ViewModelProvider) = provider[AssetsViewModel::class.java]

    @Provides
    @ForView
    fun providesAssetSelectionViewModel(provider: ViewModelProvider) = provider[AssetSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesSendAssetViewModel(provider: ViewModelProvider) = provider[SendAssetViewModel::class.java]

    @Provides
    @ForView
    fun providesSendAssetReviewViewModel(provider: ViewModelProvider) = provider[SendAssetReviewViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSelectionViewModel(provider: ViewModelProvider) = provider[SafeSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerSelectionViewModel(provider: ViewModelProvider) = provider[OwnerSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesLedgerOwnerSelectionViewModel(provider: ViewModelProvider) = provider[LedgerOwnerSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesKeystoneOwnerSelectionViewModel(provider: ViewModelProvider) = provider[KeystoneOwnerSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesSettingsViewModel(provider: ViewModelProvider) = provider[SettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesImportOwnerKeyViewModel(provider: ViewModelProvider) = provider[OwnerSeedPhraseViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerGenerateViewModel(provider: ViewModelProvider) = provider[OwnerGenerateViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerEnterNameViewModel(provider: ViewModelProvider) = provider[OwnerEnterNameViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerEditNameViewModel(provider: ViewModelProvider) = provider[OwnerEditNameViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerListViewModel(provider: ViewModelProvider) = provider[OwnerListViewModel::class.java]

    @Provides
    @ForView
    fun providesOwnerDetailsViewModel(provider: ViewModelProvider) = provider[OwnerDetailsViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSettingsViewModel(provider: ViewModelProvider) = provider[SafeSettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSettingsEditNameViewModel(provider: ViewModelProvider) = provider[SafeSettingsEditNameViewModel::class.java]

    @Provides
    @ForView
    fun providesAppSettingsViewModel(provider: ViewModelProvider) = provider[AppSettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesTransactionsViewModel(provider: ViewModelProvider) = provider[TransactionsViewModel::class.java]

    @Provides
    @ForView
    fun providesTransactionListViewModel(provider: ViewModelProvider) = provider[TransactionListViewModel::class.java]

    @Provides
    @ForView
    fun providesTransactionDetailsViewModel(provider: ViewModelProvider) = provider[TransactionDetailsViewModel::class.java]

    @Provides
    @ForView
    fun providesTransactionDetailsActionViewModel(provider: ViewModelProvider) = provider[TransactionDetailsActionViewModel::class.java]

    @Provides
    @ForView
    fun providesConfirmRejectionViewModel(provider: ViewModelProvider) = provider[ConfirmRejectionViewModel::class.java]

    @Provides
    @ForView
    fun providesTxEditFeeViewModel(provider: ViewModelProvider) = provider[TxEditFeeViewModel::class.java]

    @Provides
    @ForView
    fun providesAdvancedSafeSettingsViewModel(provider: ViewModelProvider) = provider[AdvancedSafeSettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesGetInTouchViewModel(provider: ViewModelProvider) = provider[GetInTouchViewModel::class.java]

    @Provides
    @ForView
    fun providesShareSafeViewModel(provider: ViewModelProvider) = provider[ShareSafeViewModel::class.java]

    @Provides
    @ForView
    fun providesAppFiatViewModel(provider: ViewModelProvider) = provider[AppFiatViewModel::class.java]

    @Provides
    @ForView
    fun providesUpdatesViewModel(provider: ViewModelProvider) = provider[UpdatesViewModel::class.java]

    @Provides
    @ForView
    fun providesLedgerDeviceListViewModel(provider: ViewModelProvider) = provider[LedgerDeviceListViewModel::class.java]

    @Provides
    @ForView
    fun providesLedgerSignViewModel(provider: ViewModelProvider) = provider[LedgerSignViewModel::class.java]
}
