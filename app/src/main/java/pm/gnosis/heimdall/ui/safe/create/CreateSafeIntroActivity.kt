package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_safe_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber

class CreateSafeIntroActivity : BaseActivity() {
    override fun screenId() = ScreenId.CREATE_SAFE_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_create_safe_intro)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_create_safe_intro_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_create_safe_intro_next.clicks()
            .subscribeBy(onNext = { startActivity(PairingActivity.createIntent(this)) }, onError = Timber::e)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafeIntroActivity::class.java)
    }
}
