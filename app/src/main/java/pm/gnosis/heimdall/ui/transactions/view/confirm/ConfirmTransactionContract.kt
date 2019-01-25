package pm.gnosis.heimdall.ui.transactions.view.confirm

import androidx.lifecycle.ViewModel
import androidx.annotation.StringRes
import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ConfirmTransactionContract : ViewModel() {

    abstract fun setup(
        safe: Solidity.Address,
        hash: String,
        operationalGas: BigInteger,
        dataGas: BigInteger,
        txGas: BigInteger,
        gasToken: Solidity.Address,
        gasPrice: BigInteger,
        nonce: BigInteger?,
        signature: Signature
    )

    abstract fun observe(events: Events, transaction: SafeTransaction): Observable<Result<ViewUpdate>>

    abstract fun rejectTransaction(transaction: SafeTransaction): Completable

    data class InvalidTransactionException(@StringRes val messageId: Int): IllegalArgumentException("Invalid transaction")
}
