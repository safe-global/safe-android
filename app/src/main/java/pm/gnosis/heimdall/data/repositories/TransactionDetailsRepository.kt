package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.models.Transaction
import java.math.BigInteger

interface TransactionDetailsRepository {
    fun loadTransactionDetails(id: String, address: BigInteger, transactionHash: String?): Single<TransactionDetails>
    fun loadTransactionType(transaction: Transaction): Single<TransactionType>
    fun loadTransactionDetails(transaction: Transaction): Single<TransactionDetails>
}

data class TransactionDetails(val transactionId: String?, val type: TransactionType, val data: TransactionTypeData?,
                              val transaction: Transaction, val subject: String? = null, val timestamp: Long? = null)

sealed class TransactionTypeData
data class TokenTransferData(val recipient: BigInteger, val tokens: BigInteger) : TransactionTypeData()
data class RemoveSafeOwnerData(val ownerIndex: BigInteger, val newThreshold: Int) : TransactionTypeData()
data class AddSafeOwnerData(val newOwner: BigInteger, val newThreshold: Int) : TransactionTypeData()
data class ReplaceSafeOwnerData(val oldOwnerIndex: BigInteger, val newOwner: BigInteger) : TransactionTypeData()

enum class TransactionType {
    GENERIC,
    ETHER_TRANSFER,
    TOKEN_TRANSFER,
    ADD_SAFE_OWNER,
    REMOVE_SAFE_OWNER,
    REPLACE_SAFE_OWNER,
}
