package pm.gnosis.heimdall.ui.transactions.details.base

import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.appendText
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.utils.asEthereumAddressStringOrNull
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject


abstract class BaseEditableTransactionDetailsFragment: BaseTransactionDetailsFragment() {

    @Inject
    lateinit var baseViewModel: BaseEditableTransactionDetailsContract

    protected fun updateSafeInfoTransformer(addressView: TextView) = ObservableTransformer<BigInteger, Safe> {
        it.switchMap { baseViewModel.observeSafe(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    val safeAddress = it.address.asEthereumAddressStringOrNull()
                    val shortenedSafeAddress = safeAddress?.let { " [${safeAddress.substring(0, 6)}...${safeAddress.substring(safeAddress.length - 4)}]" } ?: ""
                    val safeDetails = SpannableStringBuilder(it.name ?: "")
                            .appendText(shortenedSafeAddress, ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.light_text)))
                    addressView.text = safeDetails
                }
    }

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

    companion object {
        private const val INPUT_DELAY_MS = 500L
    }
}