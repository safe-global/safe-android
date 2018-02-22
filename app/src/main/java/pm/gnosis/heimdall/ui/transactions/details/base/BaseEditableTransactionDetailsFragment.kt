package pm.gnosis.heimdall.ui.transactions.details.base

import android.content.Intent
import android.support.v4.content.ContextCompat
import android.widget.EditText
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.utils.handleAddressBookResult
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger
import java.util.concurrent.TimeUnit


abstract class BaseEditableTransactionDetailsFragment : BaseTransactionDetailsFragment() {

    protected fun prepareInput(input: TextView): Observable<CharSequence> =
            input
                    .textChanges()
                    .debounce(INPUT_DELAY_MS, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        input.setTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue))
                        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue_alpha_70))
                    }

    protected fun setInputError(input: TextView) {
        input.setTextColor(ContextCompat.getColor(context!!, R.color.error))
        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.error_hint))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (handleQrCodeActivityResult(requestCode, resultCode, data, {
                    it.hexAsEthereumAddressOrNull()?.let {
                        onAddressProvided(it)
                    } ?: run {
                        onInvalidAddressScanned(it)
                    }
                })) return

        handleAddressBookResult(requestCode, resultCode, data, { onAddressProvided(it.address) })
    }

    protected open fun onAddressProvided(address: BigInteger) {}

    protected open fun onInvalidAddressScanned(input: String) {
        context!!.toast(R.string.invalid_ethereum_address)
    }

    companion object {
        private const val INPUT_DELAY_MS = 500L
    }

    protected fun EditText.setDefault(default: String?) {
        if (text.isNullOrBlank()) {
            setText(default)
            setSelection(default?.length ?: 0)
        }
    }
}