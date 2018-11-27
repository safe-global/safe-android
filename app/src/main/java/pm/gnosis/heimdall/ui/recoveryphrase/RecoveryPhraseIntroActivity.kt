package pm.gnosis.heimdall.ui.recoveryphrase

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_recovery_phrase_intro.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.scaleBitmapToWidth
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
import timber.log.Timber
import javax.inject.Inject

abstract class RecoveryPhraseIntroActivity : BaseActivity() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.RECOVERY_PHRASE_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_recovery_phrase_intro)
        layout_recovery_phrase_intro_next.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)
    }

    override fun onStart() {
        super.onStart()
        disposables += Single.fromCallable {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            scaleBitmapToWidth(
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_phone_dropping),
                targetWidth = displayMetrics.widthPixels
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { layout_recovery_phrase_intro_phone_drop_image.setImageBitmap(it) }, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_recovery_phrase_intro_toolbar_shadow, layout_recovery_phrase_intro_scroll)

        disposables += layout_recovery_phrase_intro_back_button.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_recovery_phrase_intro_next.clicks()
            .subscribeBy(onNext = { onNextClicked() }, onError = Timber::e)
    }

    abstract fun onNextClicked()

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }
}
