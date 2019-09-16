package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.screen_create_safe_steps.*

class CreateSafeStepsActivity : BaseActivity() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId(): ScreenId? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_create_safe_steps)
        //viewComponent().inject(this)
    }

    override fun onStart() {
        super.onStart()
        disposables += steps_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += steps_botton_panel.forwardClicks
            .subscribeBy(onNext = { startActivity(CreateSafePairingActivity.createIntent(this)) }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(steps_toolbar_shadow, steps_content_scroll)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafeStepsActivity::class.java)
    }
}