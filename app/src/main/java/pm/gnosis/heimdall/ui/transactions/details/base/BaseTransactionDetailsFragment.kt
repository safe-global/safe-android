package pm.gnosis.heimdall.ui.transactions.details.base

import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.transactions.BaseTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

abstract class BaseTransactionDetailsFragment : BaseFragment() {

    @Inject
    lateinit var baseViewModel: BaseTransactionDetailsContract

    abstract fun observeTransaction(): Observable<Result<Transaction>>
    abstract fun observeSafe(): Observable<Optional<Solidity.Address>>
    abstract fun inputEnabled(enabled: Boolean)

    protected fun updateSafeInfoTransformer(addressView: TextView) = ObservableTransformer<Solidity.Address, Safe> {
        it.switchMap { baseViewModel.observeSafe(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                val safeAddress = it.address.asEthereumAddressString()
                val shortenedSafeAddress = " [${safeAddress.substring(0, 6)}...${safeAddress.substring(safeAddress.length - 4)}]"
                val safeDetails = SpannableStringBuilder(it.name ?: "")
                    .appendText(shortenedSafeAddress, ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.light_text)))
                addressView.text = safeDetails
            }
    }

    override fun onStart() {
        super.onStart()
        (activity as? BaseTransactionActivity)?.registerFragmentObservables(this)
    }
}
