package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


interface TransactionRepository {
    fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray>
    fun loadExecuteInformation(safeAddress: BigInteger, transaction: Transaction): Single<ExecuteInformation>
    fun sign(safeAddress: BigInteger, transaction: Transaction): Single<Signature>
    fun checkSignature(safeAddress: BigInteger, transaction: Transaction, signature: Signature): Single<Pair<BigInteger, Signature>>
    fun estimateFees(safeAddress: BigInteger, transaction: Transaction, signatures: Map<BigInteger, Signature>, senderIsOwner: Boolean): Single<GasEstimate>
    fun submit(safeAddress: BigInteger, transaction: Transaction, signatures: Map<BigInteger, Signature>, senderIsOwner: Boolean, overrideGasPrice: Wei? = null): Completable

    data class ExecuteInformation(val transactionHash: String, val transaction: Transaction, val sender: BigInteger, val requiredConfirmation: Int, val owners: List<BigInteger>) {
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
}