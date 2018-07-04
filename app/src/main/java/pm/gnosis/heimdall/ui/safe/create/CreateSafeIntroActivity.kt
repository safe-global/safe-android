package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_safe_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject

class CreateSafeIntroActivity : BaseActivity() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.CREATE_SAFE_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_create_safe_intro)
        viewComponent().inject(this)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_create_safe_intro_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_create_safe_intro_next.clicks()
            .subscribeBy(onNext = { startActivity(PairingActivity.createIntent(this)) }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_create_safe_intro_toolbar_shadow, layout_create_safe_intro_content_scroll)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafeIntroActivity::class.java)
    }
}
