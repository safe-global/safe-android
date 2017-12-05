package pm.gnosis.heimdall.data.repositories

import io.reactivex.Single
import pm.gnosis.models.Transaction
import java.math.BigInteger

interface TransactionDetailsRepository {
    fun loadTransactionDetails(id: String, address: BigInteger, transactionHash: String?): Single<TransactionDetails>
    fun loadTransactionType(transaction: Transaction): Single<TransactionType>
}

data class TransactionDetails(val transactionId: String, val type: TransactionType, val data: TransactionTypeData?,
                              val transaction: Transaction, val subject: String? = null, val timestamp: Long? = null)

sealed class TransactionTypeData
data class TokenTransferData(val recipient: BigInteger, val tokens: BigInteger) : TransactionTypeData()

enum class TransactionType {
    GENERIC,
    ETHER_TRANSFER,
    TOKEN_TRANSFER
}