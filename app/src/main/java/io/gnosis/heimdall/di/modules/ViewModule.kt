package io.gnosis.heimdall.di.modules

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import io.gnosis.heimdall.di.ForView
import io.gnosis.heimdall.di.ViewContext
import io.gnosis.heimdall.ui.splash.SplashViewModel

@Module
class ViewModule(val context: Context, val viewModelProvider: Any? = null) {

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
            is Fragment -> ViewModelProviders.of(provider, factory)
            is FragmentActivity -> ViewModelProviders.of(provider, factory)
            else -> throw IllegalArgumentException("Unsupported context $provider")
        }
    }
}
