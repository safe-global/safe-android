package pm.gnosis.heimdall.ui.dialogs.share

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.include_base_share_qr_code.*
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import javax.inject.Inject

abstract class BaseShareQrCodeDialog : BaseDialog() {
    @Inject
    lateinit var qrCodeGenerator: QrCodeGenerator

    @Inject
    lateinit var eventTracker: EventTracker

    private val generateQrCodeSubject = PublishSubject.create<Unit>()

    abstract fun screenId(): ScreenId

    abstract fun data(): String?

    abstract fun shareTitle(): String

    abstract fun root(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onStart() {
        super.onStart()
        disposables += dataSourceObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { (name, data) -> onResult(name, data) }, onError = Timber::e)

        // When clicking "outside" of the dialog we should dismiss it
        disposables += root().clicks()
            .subscribeBy(onNext = { dismiss() }, onError = Timber::e)

        // Override the root clicks (we don't want to dismiss the dialog)
        disposables += include_base_share_qr_code_background.clicks()
            .subscribeBy(onError = Timber::e)

        disposables += generateQrCodeSubject
            .flatMapSingle {
                data()?.let {
                    qrCodeGenerator.generateQrCode(it)
                        .mapToResult()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { onQrCodeLoading(true) }
                        .doOnEvent { _, _ -> onQrCodeLoading(false) }
                } ?: throw IllegalStateException()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onQrCode, onError = ::onQrCodeError)

        disposables += include_base_share_qr_code_retry.clicks()
            .startWith(Unit)
            .subscribeBy(onNext = { generateQrCodeSubject.onNext(Unit) }, onError = Timber::e)

        disposables += include_base_share_qr_code_qr_code.clicks()
            .subscribeBy(onNext = { copyDataToClipboard() }, onError = Timber::e)

        disposables += include_base_share_qr_code_data.clicks()
            .subscribeBy(onNext = { copyDataToClipboard() }, onError = Timber::e)

        eventTracker.submit(Event.ScreenView(screenId()))
    }

    private fun onResult(name: String?, address: String?) {
        include_base_share_qr_code_name.text = name ?: "-"
        include_base_share_qr_code_data.text = address ?: "-"
    }

    override fun onResume() {
        super.onResume()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun onQrCode(qrCode: Bitmap) {
        include_base_share_qr_code_qr_code.visibility = View.VISIBLE
        include_base_share_qr_code_retry.visibility = View.INVISIBLE
        include_base_share_qr_code_qr_code.setImageBitmap(qrCode)
    }

    private fun onQrCodeError(throwable: Throwable) {
        include_base_share_qr_code_qr_code.visibility = View.INVISIBLE
        include_base_share_qr_code_retry.visibility = View.VISIBLE
        Timber.e(throwable)
    }

    private fun onQrCodeLoading(isLoading: Boolean) {
        include_base_share_qr_code_progress.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
    }

    private fun copyDataToClipboard() {
        nullOnThrow { data() }?.let {
            context?.copyToClipboard(shareTitle(), it, {
                onCopiedToClipboard()
            })
        }
    }

    protected open fun onCopiedToClipboard() {}

    protected abstract fun inject()

    protected abstract fun dataSourceObservable(): Observable<Pair<String?, String?>>

    companion object {
        internal const val ADDRESS_EXTRA = "extra.string.address"
    }
}
