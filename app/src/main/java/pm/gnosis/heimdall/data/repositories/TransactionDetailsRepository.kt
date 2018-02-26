package pm.gnosis.heimdall.data.repositories

import com.gojuno.koptional.Optional
import io.reactivex.Single
import pm.gnosis.models.Transaction
import java.math.BigInteger

interface TransactionDetailsRepository {
    fun loadTransactionDetails(id: String): Single<TransactionDetails>
    fun loadTransactionType(transaction: Transaction): Single<TransactionType>
    fun loadTransactionData(transaction: Transaction): Single<Optional<TransactionTypeData>>
}

data class TransactionDetails(
    val transactionId: String, val type: TransactionType, val data: TransactionTypeData?,
    val transaction: Transaction, val safe: BigInteger, val timestamp: Long, val subject: String? = null
)

sealed class TransactionTypeData
data class TokenTransferData(val recipient: BigInteger, val tokens: BigInteger) : TransactionTypeData()
data class RemoveSafeOwnerData(val ownerIndex: BigInteger, val owner: BigInteger, val newThreshold: Int) : TransactionTypeData()
data class AddSafeOwnerData(val newOwner: BigInteger, val newThreshold: Int) : TransactionTypeData()
data class ReplaceSafeOwnerData(val oldOwnerIndex: BigInteger, val owner: BigInteger, val newOwner: BigInteger) : TransactionTypeData()

enum class TransactionType {
    GENERIC,
    ETHER_TRANSFER,
    TOKEN_TRANSFER,
    ADD_SAFE_OWNER,
    REMOVE_SAFE_OWNER,
    REPLACE_SAFE_OWNER,
}
