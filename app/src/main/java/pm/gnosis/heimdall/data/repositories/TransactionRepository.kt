package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger


interface TransactionRepository {
    fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray>
    fun loadExecuteInformation(safeAddress: BigInteger, transaction: Transaction): Single<ExecuteInformation>
    fun sign(safeAddress: BigInteger, transaction: Transaction): Single<Signature>
    fun checkSignature(safeAddress: BigInteger, transaction: Transaction, signature: Signature): Single<Pair<BigInteger, Signature>>

    fun submit(
        safeAddress: BigInteger,
        transaction: Transaction,
        signatures: Map<BigInteger, Signature>,
        senderIsOwner: Boolean
    ): Completable

    data class ExecuteInformation(
        val transactionHash: String,
        val transaction: Transaction,
        val sender: BigInteger,
        val requiredConfirmation: Int,
        val owners: List<BigInteger>
    ) {
        val isOwner by lazy {
            owners.contains(sender)
        }
    }

    enum class PublishStatus {
        UNKNOWN,
        PENDING,
        FAILED,
        SUCCESS
    }

    fun observePublishStatus(id: String): Observable<PublishStatus>
    fun loadChainHash(id: String): Single<String>
    fun addLocalTransaction(safeAddress: BigInteger, transaction: Transaction, txChainHash: String): Single<String>
    fun loadExecutableTransaction(
        safeAddress: BigInteger,
        innerTransaction: Transaction,
        signatures: Map<BigInteger, Signature>,
        senderIsOwner: Boolean
    ): Single<Transaction>
}
