package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_safe_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.authenticator.SetupAuthenticatorActivity
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
import timber.log.Timber
import javax.inject.Inject

class CreateSafeIntroActivity : BaseActivity() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.NEW_SAFE_START

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_create_safe_intro)
        viewComponent().inject(this)

        layout_create_safe_intro_third_text.text = Html.fromHtml(getString(R.string.safe_intro_message_3))
        layout_create_safe_intro_next.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_create_safe_intro_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_create_safe_intro_next.clicks()
            .subscribeBy(onNext = { startActivity(SetupAuthenticatorActivity.createIntent(this, null)) }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_create_safe_intro_toolbar_shadow, layout_create_safe_intro_content_scroll)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafeIntroActivity::class.java)
    }
}
