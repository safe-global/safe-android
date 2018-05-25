package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.model.Solidity
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger


interface TransactionExecutionRepository {
    fun calculateHash(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<ByteArray>
    fun loadExecuteInformation(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<ExecuteInformation>
    fun sign(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Signature>
    fun checkSignature(safeAddress: Solidity.Address, transaction: SafeTransaction, signature: Signature): Single<Pair<Solidity.Address, Signature>>
    fun estimateFees(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<FeeEstimate>

    fun observePublishStatus(id: String): Observable<PublishStatus>
    fun loadChainHash(id: String): Single<String>
    fun addLocalTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction, txChainHash: String): Single<String>
    fun loadExecutableTransaction(
        safeAddress: Solidity.Address,
        innerTransaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<Transaction>

    fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Completable

    data class ExecuteInformation(
        val transactionHash: String,
        val transaction: SafeTransaction,
        val requiredConfirmation: Int,
        val owners: List<Solidity.Address>,
        val gasPrice: BigInteger,
        val estimate: BigInteger,
        val balance: Wei
    )

    enum class PublishStatus {
        UNKNOWN,
        PENDING,
        FAILED,
        SUCCESS
    }

    enum class Operation {
        CALL,
        DELEGATE_CALL
    }
}
