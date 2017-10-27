package pm.gnosis.heimdall.data.repositories

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.impls.GnosisMultisigTransaction
import java.math.BigInteger

interface TransactionDetailRepository {
    fun loadTransactionDetails(address: BigInteger, transactionId: BigInteger): Observable<GnosisMultisigTransaction>
}