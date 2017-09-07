package pm.gnosis.android.app.authenticator.di.component

import android.content.Context
import dagger.Component
import pm.gnosis.android.app.authenticator.di.ForView
import pm.gnosis.android.app.authenticator.di.ViewContext
import pm.gnosis.android.app.authenticator.di.module.ViewModule
import pm.gnosis.android.app.authenticator.ui.MainActivity
import pm.gnosis.android.app.authenticator.ui.account.AccountFragment
import pm.gnosis.android.app.authenticator.ui.account.AccountPresenter
import pm.gnosis.android.app.authenticator.ui.authenticate.AuthenticateFragment
import pm.gnosis.android.app.authenticator.ui.multisig.MultisigFragment
import pm.gnosis.android.app.authenticator.ui.splash.SplashActivity
import pm.gnosis.android.app.authenticator.ui.tokens.TokensFragment
import pm.gnosis.android.app.authenticator.ui.transactiondetails.TransactionDetailsActivity

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
