package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger


abstract class BaseCreateSafeTransactionProgressDialog : BaseDialog() {

    protected var safeAddress: BigInteger? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        safeAddress = arguments?.getString(SAFE_ADDRESS_EXTRA)?.hexAsEthereumAddressOrNull()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_base_create_safe_transaction_progress, container, false)

    abstract fun createTransaction(): Single<Transaction>

    abstract fun showTransaction(safe: BigInteger?, transaction: Transaction)

    override fun onStart() {
        super.onStart()
        disposables += createTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                dismiss()
            }
            .subscribe({
                showTransaction(safeAddress, it)
            }, {
                context!!.errorToast(it)
            })
    }

    companion object {
        private const val SAFE_ADDRESS_EXTRA = "extra.string.safe_address"

        fun createBundle(safeAddress: BigInteger) = Bundle().apply {
            putString(SAFE_ADDRESS_EXTRA, safeAddress.asEthereumAddressString())
        }
    }
}