package pm.gnosis.heimdall.ui.dialogs.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_fingerprint_scan.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.common.utils.vibrate
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.impls.AuthenticationError
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import timber.log.Timber
import javax.inject.Inject

class FingerprintDialog : BaseDialog() {
    @Inject
    lateinit var encryptionManager: EncryptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dialog_fingerprint_scan, container, false)

    override fun onStart() {
        super.onStart()
        disposables += encryptionManager.observeFingerprintForSetup()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = ::onFingerprintResult, onError = ::onFingerprintUnrecoverableError)
    }

    private fun onFingerprintResult(isSuccessful: Boolean) {
        dialog_fingerprint_scan.visibility = View.INVISIBLE
        if (isSuccessful) {
            context!!.toast(R.string.fingerprint_registered)
            dismiss()
        } else {
            dialog_fingerprint_scan.visibility = View.VISIBLE
            context!!.vibrate(200)
        }
    }

    private fun onFingerprintUnrecoverableError(throwable: Throwable) {
        Timber.e(throwable)
        val message = (throwable as? AuthenticationError).let { it?.errString } ?: getString(R.string.unknown_error)
        context!!.toast(message)
        dismiss()
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!))
                .applicationComponent(HeimdallApplication[context!!].component)
                .build()
                .inject(this)
    }

    companion object {
        fun create() = FingerprintDialog()
    }
}
