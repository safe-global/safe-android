package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Flowable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.Transaction
import java.math.BigInteger


abstract class SafeTransactionsContract: ViewModel() {
    abstract fun setup(address: BigInteger)

    abstract fun observeTransactions(): Flowable<out Result<Adapter.Data<String>>>

    abstract fun loadTransactionDetails(id: String): Single<Pair<TransactionDetails, TransferInfo?>>

    abstract fun transactionSelected(it: Transaction): Single<Intent>

    data class TransferInfo(val amount: String, val symbol: String?)

}