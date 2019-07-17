package pm.gnosis.heimdall.ui.transactions.view.review

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class ReviewTransactionContract : ViewModel() {

    abstract fun setup(safe: Solidity.Address, referenceId: Long?, sessionId: String?)

    abstract fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>>

    abstract fun observeSessionInfo(): Observable<Result<SessionInfo>>

    abstract fun cancelReview()

    data class SessionInfo(val dappName: String?, val dappUrl: String?, val iconUrl: String?)
}
