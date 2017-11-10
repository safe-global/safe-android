package pm.gnosis.heimdall.data.repositories

import io.reactivex.Observable
import pm.gnosis.models.Wei
import java.math.BigInteger

interface TransactionDetailsRepository {
    fun loadTransactionDetails(descriptionHash: String, address: BigInteger, transactionHash: String?): Observable<TransactionDetails>
}

data class TransactionDetails(val type: TransactionType, val descriptionHash: String, val transactionHash: String? = null, val subject: String? = null, val timestamp: Long? = null) {
    companion object {
        fun unknown(descriptionHash: String) = TransactionDetails(UnknownTransactionType(null), descriptionHash)
    }
}

sealed class TransactionType
data class UnknownTransactionType(val data: String?) : TransactionType()
data class SafeTransaction(val to: BigInteger, val value: Wei, val data: String, val operation: BigInteger, val nonce: BigInteger) : TransactionType()
data class EtherTransfer(val address: BigInteger, val value: Wei) : TransactionType()
data class TokenTransfer(val tokenAddress: BigInteger, val recipient: BigInteger, val tokens: BigInteger) : TransactionType()
data class SafeChangeDailyLimit(val newDailyLimit: BigInteger) : TransactionType()
data class SafeReplaceOwner(val owner: BigInteger, val newOwner: BigInteger) : TransactionType()
data class SafeAddOwner(val owner: BigInteger) : TransactionType()
data class SafeRemoveOwner(val owner: BigInteger) : TransactionType()
data class SafeChangeConfirmations(val newConfirmations: BigInteger) : TransactionType()