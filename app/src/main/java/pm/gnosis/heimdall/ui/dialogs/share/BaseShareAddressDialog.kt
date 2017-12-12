package pm.gnosis.heimdall.ui.dialogs.share

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_address_share.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

abstract class BaseShareAddressDialog : BaseDialog() {
    @Inject
    lateinit var qrCodeGenerator: QrCodeGenerator

    private val generateQrCodeSubject = PublishSubject.create<Unit>()

    protected lateinit var address: BigInteger

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        val address = arguments?.getString(ADDRESS_EXTRA)?.hexAsEthereumAddressOrNull()
        if (address == null) {
            dismiss()
        } else {
            this.address = address
        }
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dialog_address_share, container, false)

    override fun onStart() {
        super.onStart()
        disposables += addressSourceObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { (name, address) -> onResult(name, address) })

        // When clicking "outside" of the dialog we should dismiss it
        disposables += dialog_address_share_root.clicks()
                .subscribeBy(onNext = { dismiss() })

        // Override the root clicks (we don't want to dismiss the dialog)
        disposables += dialog_address_share_background.clicks()
                .subscribeBy()

        disposables += generateQrCodeSubject
                .flatMapSingle {
                    qrCodeGenerator.generateQrCode(address.asEthereumAddressString())
                            .mapToResult()
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { onQrCodeLoading(true) }
                            .doOnEvent { _, _ -> onQrCodeLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = ::onQrCode, onError = ::onQrCodeError)

        disposables += dialog_address_share_retry.clicks()
                .startWith(Unit)
                .subscribeBy(onNext = { generateQrCodeSubject.onNext(Unit) })

        disposables += dialog_share_qr_code.clicks()
                .subscribeBy(onNext = { copyAddressToClipboard() })

        disposables += dialog_address_share_address.clicks()
                .subscribeBy(onNext = { copyAddressToClipboard() })

        disposables += dialog_address_share_share.clicks()
                .subscribeBy(onNext = {
                    address.asEthereumAddressStringOrNull()?.let {
                        context?.shareExternalText(it, R.string.address)
                    }
                })
    }

    private fun onResult(name: String?, address: BigInteger) {
        dialog_address_share_name.text = name ?: "-"
        dialog_address_share_address.text = address.asEthereumAddressStringOrNull() ?: "-"
    }

    override fun onResume() {
        super.onResume()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun onQrCode(qrCode: Bitmap) {
        dialog_share_qr_code.visibility = View.VISIBLE
        dialog_address_share_retry.visibility = View.INVISIBLE
        dialog_share_qr_code.setImageBitmap(qrCode)
    }

    private fun onQrCodeError(throwable: Throwable) {
        dialog_share_qr_code.visibility = View.INVISIBLE
        dialog_address_share_retry.visibility = View.VISIBLE
        Timber.e(throwable)
    }

    private fun onQrCodeLoading(isLoading: Boolean) {
        dialog_address_share_progress.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
    }

    private fun copyAddressToClipboard() {
        address.asEthereumAddressStringOrNull()?.let {
            context?.copyToClipboard(getString(R.string.address), it, {
                context?.toast(R.string.address_clipboard_success)
            })
        }
    }

    protected abstract fun inject()

    protected abstract fun addressSourceObservable(): Observable<Pair<String?, BigInteger>>

    companion object {
        const val ADDRESS_EXTRA = "extra.string.address"
    }
}
