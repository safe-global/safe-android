package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_finish_setup.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.startActivity
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.safe.add.AddSafeActivity
import pm.gnosis.heimdall.ui.safe.overview.SafesOverviewActivity

class SetupSafeIntroActivity : BaseActivity() {

    override fun screenId() = ScreenId.SETUP_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_finish_setup)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_finish_setup_safe.clicks()
            .subscribeBy(onNext = {
                startActivity(SafesOverviewActivity.createIntent(this), clearStack = true)
                startActivity(AddSafeActivity.createIntent(this))
                finish()
            })

        disposables += layout_finish_setup_skip.clicks()
            .subscribeBy(onNext = {
                startActivity(SafesOverviewActivity.createIntent(this), clearStack = true)
                finish()
            })
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, SetupSafeIntroActivity::class.java)
    }
}
