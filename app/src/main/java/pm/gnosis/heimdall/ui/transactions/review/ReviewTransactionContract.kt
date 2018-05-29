package pm.gnosis.heimdall.ui.transactions.review

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.Result

abstract class ReviewTransactionContract : ViewModel() {

    abstract fun setup(safe: Solidity.Address)

    abstract fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>>

    data class Events(val retry: Observable<Unit>, val requestConfirmations: Observable<Unit>, val submit: Observable<Unit>)

    sealed class ViewUpdate {
        data class TransactionInfo(val viewHolder: TransactionInfoViewHolder) : ViewUpdate()
        data class Estimate(val fees: Wei, val balance: Wei) : ViewUpdate()
        object EstimateError : ViewUpdate()
        data class Confirmations(val isReady: Boolean) : ViewUpdate()
        object ConfirmationsRequested : ViewUpdate()
        object ConfirmationsError : ViewUpdate()
        object TransactionRejected : ViewUpdate()
        data class TransactionSubmitted(val success: Boolean) : ViewUpdate()
    }
}
