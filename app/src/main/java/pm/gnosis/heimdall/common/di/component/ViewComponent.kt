package pm.gnosis.heimdall.common.di.component

import dagger.Component
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.heimdall.ui.multisig.details.MultisigDetailsActivity
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoFragment
import pm.gnosis.heimdall.ui.multisig.overview.MultisigOverviewFragment
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
    fun inject(fragment: MultisigInfoFragment)
    fun inject(fragment: MultisigOverviewFragment)
    fun inject(fragment: TokensFragment)

    // Activities

    fun inject(activity: GenerateMnemonicActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: RestoreAccountActivity)
    fun inject(activity: SecurityActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TransactionDetailsActivity)
}
