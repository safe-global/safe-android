package pm.gnosis.heimdall.ui.safe.recover.safe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_recover_safe_intro.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject

class RecoverSafeIntroActivity: BaseActivity() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.RECOVER_SAFE_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_recover_safe_intro)
    }

    override fun onStart() {
        super.onStart()

        disposables += toolbarHelper.setupShadow(layout_recover_safe_intro_toolbar_shadow, layout_recover_safe_intro_scroll)

        disposables += layout_recover_safe_intro_back_button.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_recover_safe_intro_next.clicks()
            .subscribeBy(onNext = { onNextClicked() }, onError = Timber::e)
    }

    private fun onNextClicked() {
        startActivity(CheckSafeActivity.createIntent(this))
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) =
            Intent(context, RecoverSafeIntroActivity::class.java)
    }
}
