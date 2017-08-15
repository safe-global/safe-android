package pm.gnosis.android.app.wallet.di.component

import android.content.Context
import dagger.Component
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.di.ViewContext
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.MainActivity
import pm.gnosis.android.app.wallet.ui.transactiondetails.TransactionDetailsActivity
import pm.gnosis.android.app.wallet.ui.account.AccountFragment
import pm.gnosis.android.app.wallet.ui.account.AccountPresenter
import pm.gnosis.android.app.wallet.ui.multisig.MultisigFragment
import pm.gnosis.android.app.wallet.ui.authenticate.AuthenticateFragment
import pm.gnosis.android.app.wallet.ui.splash.SplashActivity
import pm.gnosis.android.app.wallet.ui.tokens.TokensFragment

@ForView
@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(ViewModule::class))
interface ViewComponent {
    @ViewContext fun context(): Context
    fun accountPresenter(): AccountPresenter

    fun inject(activity: SplashActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: TransactionDetailsActivity)
    fun inject(fragment: AccountFragment)
    fun inject(fragment: MultisigFragment)
    fun inject(fragment: AuthenticateFragment)
    fun inject(fragment: TokensFragment)
}
