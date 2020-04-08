package io.gnosis.safe.di.modules

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import io.gnosis.safe.di.ForView
import io.gnosis.safe.di.ViewContext
import io.gnosis.safe.ui.splash.SplashViewModel

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
    fun providesSplashViewModel(provider: ViewModelProvider) = provider[SplashViewModel::class.java]

    @Provides
    @ForView
    fun providesViewModelProvider(factory: ViewModelProvider.Factory): ViewModelProvider {
        return when (val provider = viewModelProvider ?: context) {
            is Fragment -> ViewModelProvider(provider, factory)
            is FragmentActivity -> ViewModelProvider(provider, factory)
            else -> throw IllegalArgumentException("Unsupported context $provider")
        }
    }
}
