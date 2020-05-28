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
import io.gnosis.safe.ui.dialogs.EnsInputViewModel
import io.gnosis.safe.ui.safe.add.AddSafeNameViewModel
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.balances.SafeBalancesViewModel
import io.gnosis.safe.ui.safe.balances.coins.CoinsViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionAdapter
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.gnosis.safe.ui.safe.settings.SettingsViewModel
import io.gnosis.safe.ui.safe.settings.safe.SafeSettingsViewModel
import io.gnosis.safe.ui.splash.SplashViewModel
import io.gnosis.safe.ui.transaction.TransactionsViewModel
import java.lang.ref.WeakReference

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
    fun providesAddSafeViewModel(provider: ViewModelProvider) = provider[AddSafeViewModel::class.java]

    @Provides
    @ForView
    fun providesSplashViewModel(provider: ViewModelProvider) = provider[SplashViewModel::class.java]

    @Provides
    @ForView
    fun providesEnsInputViewModel(provider: ViewModelProvider) = provider[EnsInputViewModel::class.java]

    @Provides
    @ForView
    fun providesAddSafeNameViewModel(provider: ViewModelProvider) = provider[AddSafeNameViewModel::class.java]

    @Provides
    @ForView
    fun providesCoinsViewModel(provider: ViewModelProvider) = provider[CoinsViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeBalancesViewModel(provider: ViewModelProvider) = provider[SafeBalancesViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSelectionViewModel(provider: ViewModelProvider) = provider[SafeSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesSettingsViewModel(provider: ViewModelProvider) = provider[SettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSettingsViewModel(provider: ViewModelProvider) = provider[SafeSettingsViewModel::class.java]

    @Provides
    @ForView
    fun providesTransactionsViewModel(provider: ViewModelProvider) = provider[TransactionsViewModel::class.java]
}
