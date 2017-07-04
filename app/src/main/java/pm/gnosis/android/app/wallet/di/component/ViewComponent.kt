package pm.gnosis.android.app.wallet.di.component

import android.content.Context
import dagger.Component
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.di.ViewContext
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.MainActivity

@ForView
@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(ViewModule::class))
interface ViewComponent {
    @ViewContext fun Context(): Context

    fun inject(activity: MainActivity)
}