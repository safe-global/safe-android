package pm.gnosis.heimdall.common.di.components

import dagger.Component
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.ui.account.AccountFragment
import pm.gnosis.heimdall.ui.authenticate.AuthenticateFragment
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.heimdall.ui.onboarding.GenerateMnemonicActivity
import pm.gnosis.heimdall.ui.onboarding.RestoreAccountActivity
import pm.gnosis.heimdall.ui.safe.details.SafeDetailsActivity
import pm.gnosis.heimdall.ui.safe.details.info.SafeInfoFragment
import pm.gnosis.heimdall.ui.safe.details.transactions.SafeTransactionsFragment
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewFragment
import pm.gnosis.heimdall.ui.security.SecurityActivity
import pm.gnosis.heimdall.ui.settings.SettingsActivity
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.tokens.addtoken.AddTokenActivity
import pm.gnosis.heimdall.ui.tokens.overview.TokensFragment
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
    fun inject(fragment: SafeInfoFragment)
    fun inject(fragment: SafeOverviewFragment)
    fun inject(fragment: SafeTransactionsFragment)
    fun inject(fragment: TokensFragment)

    // Activities

    fun inject(activity: AddTokenActivity)
    fun inject(activity: GenerateMnemonicActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: RestoreAccountActivity)
    fun inject(activity: SafeDetailsActivity)
    fun inject(activity: SecurityActivity)
    fun inject(activity: SettingsActivity)
    fun inject(activity: SplashActivity)
    fun inject(activity: TransactionDetailsActivity)
}
