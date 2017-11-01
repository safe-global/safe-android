package pm.gnosis.heimdall.data.repositories

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.impls.TransactionDetails
import java.math.BigInteger

interface TransactionDetailRepository {
    fun loadTransactionDetails(address: BigInteger, transactionHash: String, descriptionHash: String): Observable<TransactionDetails>
}