package pm.gnosis.heimdall.common.di.modules

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import dagger.Module
import dagger.Provides
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.ViewContext
import pm.gnosis.heimdall.ui.account.AccountContract
import pm.gnosis.heimdall.ui.authenticate.AuthenticateContract
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicContract
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountContract
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsContract
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoContract
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsContract
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewContract
import pm.gnosis.heimdall.ui.security.SecurityContract
import pm.gnosis.heimdall.ui.settings.SettingsContract
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.tokens.addtoken.AddTokenContract
import pm.gnosis.heimdall.ui.tokens.overview.TokensContract
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsContract

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
    fun providesAuthenticateContract(provider: ViewModelProvider) = provider[AuthenticateContract::class.java]

    @Provides
    @ForView
    fun providesAddTokenContract(provider: ViewModelProvider) = provider[AddTokenContract::class.java]

    @Provides
    @ForView
    fun providesAccountContract(provider: ViewModelProvider) = provider[AccountContract::class.java]

    @Provides
    @ForView
    fun providesGenerateMnemonicContract(provider: ViewModelProvider) = provider[GenerateMnemonicContract::class.java]

    @Provides
    @ForView
    fun providesSafeDetailsContract(provider: ViewModelProvider) = provider[SafeDetailsContract::class.java]

    @Provides
    @ForView
    fun providesSafeInfoContract(provider: ViewModelProvider) = provider[SafeInfoContract::class.java]

    @Provides
    @ForView
    fun providesSafeOverviewContract(provider: ViewModelProvider) = provider[SafeOverviewContract::class.java]

    @Provides
    @ForView
    fun providesSafeTransactionsContract(provider: ViewModelProvider) = provider[SafeTransactionsContract::class.java]

    @Provides
    @ForView
    fun providesRestoreAccountContract(provider: ViewModelProvider) = provider[RestoreAccountContract::class.java]

    @Provides
    @ForView
    fun providesSecurityContract(provider: ViewModelProvider) = provider[SecurityContract::class.java]

    @Provides
    @ForView
    fun providesSettingsContract(provider: ViewModelProvider) = provider[SettingsContract::class.java]

    @Provides
    @ForView
    fun providesSplashContract(provider: ViewModelProvider) = provider[SplashContract::class.java]

    @Provides
    @ForView
    fun providesTokensContract(provider: ViewModelProvider) = provider[TokensContract::class.java]

    @Provides
    @ForView
    fun providesTransactionDetailsContract(provider: ViewModelProvider) = provider[TransactionDetailsContract::class.java]

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
