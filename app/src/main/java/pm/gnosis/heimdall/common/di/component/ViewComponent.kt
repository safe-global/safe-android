package pm.gnosis.heimdall.common.di.component

import dagger.Component
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.heimdall.ui.multisig.MultisigFragment
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicActivity
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountActivity
import pm.gnosis.heimdall.ui.security.SecurityActivity
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.TokensFragment
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity

@ForView
@Component(
        dependencies = arrayOf(ApplicationComponent::class),
        modules = arrayOf(ViewModule::class)
)
interface ViewComponent {
    // Fragments

    fun inject(fragment: AccountFragment)
    fun inject(fragment: AuthenticateFragment)
    fun inject(fragment: MultisigFragment)
    fun inject(fragment: TokensFragment)

    // Activities

    fun inject(activity: GenerateMnemonicActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: SecurityActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TransactionDetailsActivity)
    fun inject(activity: RestoreAccountActivity)
}
