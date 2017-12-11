package pm.gnosis.heimdall.ui.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.models.Transaction


abstract class ViewTransactionContract: ViewModel() {
    abstract fun checkTransactionType(transaction: Transaction): Single<TransactionType>
}