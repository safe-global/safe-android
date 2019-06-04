package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Intent
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfo
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class SafeTransactionsContract : ViewModel() {
    abstract fun setup(address: Solidity.Address)

    abstract fun observeTransactions(): Flowable<out Result<Adapter.Data<AdapterEntry>>>

    abstract fun loadTransactionInfo(id: String): Single<TransactionInfo>

    abstract fun loadTokenInfo(token: Solidity.Address): Single<ERC20Token>

    abstract fun observeTransactionStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus>

    abstract fun transactionSelected(id: String): Single<Intent>

    sealed class AdapterEntry(@IdRes val type: Int) {
        data class Header(val title: String): AdapterEntry(R.id.adapter_entry_header)
        data class Transaction(val id: String): AdapterEntry(R.id.adapter_entry_transaction)
    }
}
