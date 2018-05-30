package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.URLSpan
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_pairing.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.svalinn.common.utils.*
import timber.log.Timber
import javax.inject.Inject

class PairingActivity : BaseActivity() {
    override fun screenId() = ScreenId.PAIRING

    @Inject
    lateinit var viewModel: PairingContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_pairing)

        layout_pairing_extension_link.apply {
            val linkDrawable = ContextCompat.getDrawable(context, R.drawable.ic_external_link)!!
            linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
            this.text = SpannableStringBuilder("")
                .appendText(getString(R.string.share_browser_extension), URLSpan("https://gnosis.pm"))
                .append(" ")
                .appendText(" ", ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE))
            setOnClickListener { shareExternalText("https://gnosis.pm", "Browser Extension URL") }
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_pairing_scan.clicks()
            .subscribeBy(
                onNext = { QRCodeScanActivity.startForResult(this) },
                onError = Timber::e
            )

        disposables += layout_create_safe_intro_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, onQrCodeResult = { disposables += pair(it) })
    }

    private fun pair(payload: String) =
        viewModel.pair(payload)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onPairingLoading(true) }
            .doFinally { onPairingLoading(false) }
            .subscribeBy(onSuccess = {
                toast(R.string.devices_paired_successfuly)
                startActivity(SafeRecoveryPhraseActivity.createIntent(this, it))
            }, onError = {
                snackbar(layout_pairing_coordinator, R.string.error_pairing_devices)
                Timber.e(it)
            })

    private fun onPairingLoading(isLoading: Boolean) {
        layout_pairing_scan.isEnabled = !isLoading
        layout_pairing_progress_bar.visible(isLoading)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, PairingActivity::class.java)
    }
}
