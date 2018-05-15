package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.toast
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

        disposables += layout_pairing_scan.clicks()
            .subscribeBy(onNext = { QRCodeScanActivity.startForResult(this) }, onError = Timber::e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, onQrCodeResult = { disposables += pair(it) })
    }

    private fun pair(payload: String) = viewModel.pair(payload)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            layout_pairing_scan.isEnabled = false
            layout_pairing_progress_bar.visibility = View.VISIBLE
        }
        .doOnSuccess {
            layout_pairing_scan.isEnabled = true
            layout_pairing_progress_bar.visibility = View.GONE
        }
        .subscribeBy(onSuccess = {
            toast("Devices paired successfully")
            startActivity(SafeRecoveryPhraseActivity.createIntent(this, it))
        }, onError = {
            snackbar(layout_pairing_coordinator, "Error pairing devices")
            Timber.e(it)
        })

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
