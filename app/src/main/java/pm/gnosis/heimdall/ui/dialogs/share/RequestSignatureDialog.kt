package pm.gnosis.heimdall.ui.dialogs.share

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.dialog_request_signature.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger

class RequestSignatureDialog : BaseShareQrCodeDialog() {

    private lateinit var transactionHash: String
    private lateinit var transaction: Transaction
    private lateinit var safe: BigInteger

    override fun onCreate(savedInstanceState: Bundle?) {
        val transactionHash = arguments?.getString(TRANSACTION_HASH_EXTRA)
        val transaction = arguments?.getParcelable<TransactionParcelable>(TRANSACTION_EXTRA)?.transaction
        val safe = arguments?.getString(SAFE_EXTRA)?.hexAsEthereumAddressOrNull()
        if (transactionHash == null || transaction == null || safe == null) {
            dismiss()
        } else {
            this.transactionHash = transactionHash
            this.transaction = transaction
            this.safe = safe
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dialog_request_signature, container, false)

    override fun data(): String =
            GnoSafeUrlParser.signRequest(transactionHash, safe, transaction.address, transaction.value, transaction.data, transaction.nonce!!)

    override fun shareTitle(): String = getString(R.string.transaction_hash)

    override fun root(): View = dialog_request_signature_root

    override fun screenId() = ScreenId.DIALOG_REQUEST_SIGNATURE

    override fun inject() {
        DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!))
                .applicationComponent(HeimdallApplication[context!!].component)
                .build()
                .inject(this)
    }

    override fun dataSourceObservable(): Observable<Pair<String?, String?>> =
            Observable.just(getString(R.string.request_signature) to transactionHash)

    override fun onStart() {
        super.onStart()
        disposables += dialog_request_signature_scan.clicks()
                .subscribe {
                    // We use the parent activity, so that it handles the result
                    activity?.scanQrCode()
                    dismiss()
                }
    }

    override fun onCopiedToClipboard() {
        context?.toast(R.string.data_clipboard_success)
    }

    companion object {
        private const val TRANSACTION_HASH_EXTRA = "extra.string.transaction_hash"
        private const val TRANSACTION_EXTRA = "extra.parcelable.transaction"
        private const val SAFE_EXTRA = "extra.string.safe"

        fun create(transactionHash: String, transaction: Transaction, safe: BigInteger): RequestSignatureDialog {
            val bundle = Bundle()
            bundle.putString(TRANSACTION_HASH_EXTRA, transactionHash)
            bundle.putParcelable(TRANSACTION_EXTRA, transaction.parcelable())
            bundle.putString(SAFE_EXTRA, safe.asEthereumAddressString())
            return RequestSignatureDialog().apply { arguments = bundle }
        }
    }
}
