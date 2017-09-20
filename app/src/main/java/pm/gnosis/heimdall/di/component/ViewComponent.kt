package pm.gnosis.heimdall.di.component

import android.content.Context
import dagger.Component
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.di.module.ViewModule
import pm.gnosis.heimdall.ui.MainActivity
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.account.AccountPresenter
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.multisig.MultisigFragment
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicActivity
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.TokensFragment
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity

@ForView
@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(ViewModule::class))
interface ViewComponent {
    @ViewContext fun context(): Context
    fun accountPresenter(): AccountPresenter

    fun inject(activity: SplashActivity)
    fun inject(activity: GenerateMnemonicActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: TransactionDetailsActivity)
    fun inject(fragment: AccountFragment)
    fun inject(fragment: MultisigFragment)
    fun inject(fragment: AuthenticateFragment)
    fun inject(fragment: TokensFragment)
}
