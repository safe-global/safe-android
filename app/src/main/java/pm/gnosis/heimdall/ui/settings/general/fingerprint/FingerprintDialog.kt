package pm.gnosis.heimdall.ui.settings.general.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_fingerprint_scan.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.vibrate
import pm.gnosis.svalinn.security.AuthenticationError
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import javax.inject.Inject

class FingerprintDialog : BaseDialog() {
    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var eventTracker: EventTracker

    var successListener: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_fingerprint_scan, container, false)

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.SETTINGS_ENABLE_FINGERPRINT))

        disposables += dialog_fingerprint_scan_alpha_background.clicks()
            .subscribeBy {
                dismiss()
                successListener?.invoke(false)
            }
        disposables += encryptionManager.observeFingerprintForSetup()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onFingerprintResult, onError = ::onFingerprintUnrecoverableError)
    }

    private fun onFingerprintResult(isSuccessful: Boolean) {
        if (isSuccessful) {
            dismiss()
            successListener?.invoke(true)
        } else {
            context!!.vibrate(200)
        }
    }

    private fun onFingerprintUnrecoverableError(throwable: Throwable) {
        Timber.e(throwable)
        val message = (throwable as? AuthenticationError)?.errString ?: getString(R.string.unknown_error)
        context!!.toast(message)
        dismiss()
        successListener?.invoke(false)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }

    companion object {
        fun create() = FingerprintDialog()
    }
}
