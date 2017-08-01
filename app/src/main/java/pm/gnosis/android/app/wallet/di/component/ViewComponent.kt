package pm.gnosis.android.app.wallet.di.component

import android.content.Context
import dagger.Component
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.di.ViewContext
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.GenerateTransactionActivity
import pm.gnosis.android.app.wallet.ui.MainActivity
import pm.gnosis.android.app.wallet.ui.TransactionDetailsActivity

@ForView
@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(ViewModule::class))
interface ViewComponent {
    @ViewContext fun Context(): Context

    fun inject(activity: MainActivity)
    fun inject(activity: TransactionDetailsActivity)
    fun inject(activity: GenerateTransactionActivity)
}
