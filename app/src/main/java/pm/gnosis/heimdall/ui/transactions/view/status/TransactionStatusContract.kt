package pm.gnosis.heimdall.ui.transactions.view.status

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import java.math.BigInteger

abstract class TransactionStatusContract : ViewModel() {
    abstract fun observeUpdates(id: String): Observable<ViewUpdate>

    abstract fun observeStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus>

    sealed class ViewUpdate {
        data class Params(val hash: String, val submitted: Long, val gasCosts: BigInteger, val gasToken: ERC20Token, @StringRes val type: Int) :
            ViewUpdate()

        data class Details(val viewHolder: TransactionInfoViewHolder) : ViewUpdate()
    }
}
