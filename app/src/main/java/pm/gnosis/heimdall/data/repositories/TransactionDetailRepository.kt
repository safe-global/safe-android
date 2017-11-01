package pm.gnosis.heimdall.data.repositories

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.impls.TransactionDetails
import java.math.BigInteger

interface TransactionDetailRepository {
    fun loadTransactionDetails(descriptionHash: String, address: BigInteger, transactionHash: String?): Observable<TransactionDetails>
}