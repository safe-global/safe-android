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
import io.gnosis.safe.ui.safe.selection.SafeSelectionAdapter
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import java.lang.ref.WeakReference

@Module
class ViewModule(
    private val context: Context,
    private val viewModelProvider: Any? = null) {

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
    fun providesSafeSelectionViewModel(provider: ViewModelProvider) = provider[SafeSelectionViewModel::class.java]

    @Provides
    @ForView
    fun providesSafeSelectionAdapter(safeSelectionViewModel: SafeSelectionViewModel) =
        SafeSelectionAdapter(WeakReference(safeSelectionViewModel))
}
