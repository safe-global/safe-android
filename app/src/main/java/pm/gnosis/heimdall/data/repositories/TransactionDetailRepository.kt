package pm.gnosis.heimdall.data.repositories

import io.reactivex.Observable
import pm.gnosis.models.Wei
import java.math.BigInteger

interface TransactionDetailRepository {
    fun loadTransactionDetails(descriptionHash: String, address: BigInteger, transactionHash: String?): Observable<TransactionDetails>
}

sealed class TransactionDetails
data class UnknownTransactionDetails(val data: String?) : TransactionDetails()
data class GenericTransactionDetails(val to: BigInteger, val value: Wei, val data: String, val operation: BigInteger, val nonce: BigInteger) : TransactionDetails()
data class EtherTransfer(val address: BigInteger, val value: Wei) : TransactionDetails()
data class TokenTransfer(val tokenAddress: BigInteger, val recipient: BigInteger, val tokens: BigInteger) : TransactionDetails()
data class SafeChangeDailyLimit(val newDailyLimit: BigInteger) : TransactionDetails()
data class SafeReplaceOwner(val owner: BigInteger, val newOwner: BigInteger) : TransactionDetails()
data class SafeAddOwner(val owner: BigInteger) : TransactionDetails()
data class SafeRemoveOwner(val owner: BigInteger) : TransactionDetails()
data class SafeChangeConfirmations(val newConfirmations: BigInteger) : TransactionDetails()