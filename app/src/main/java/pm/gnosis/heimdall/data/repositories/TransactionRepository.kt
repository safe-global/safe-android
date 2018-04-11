package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature


interface TransactionRepository {
    fun calculateHash(safeAddress: Solidity.Address, transaction: Transaction): Single<ByteArray>
    fun loadExecuteInformation(safeAddress: Solidity.Address, transaction: Transaction): Single<ExecuteInformation>
    fun sign(safeAddress: Solidity.Address, transaction: Transaction): Single<Signature>
    fun checkSignature(safeAddress: Solidity.Address, transaction: Transaction, signature: Signature): Single<Pair<Solidity.Address, Signature>>
    fun estimateFees(
        safeAddress: Solidity.Address,
        transaction: Transaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<FeeEstimate>

    fun observePublishStatus(id: String): Observable<PublishStatus>
    fun loadChainHash(id: String): Single<String>
    fun addLocalTransaction(safeAddress: Solidity.Address, transaction: Transaction, txChainHash: String): Single<String>
    fun loadExecutableTransaction(
        safeAddress: Solidity.Address,
        innerTransaction: Transaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<Transaction>

    fun submit(
        safeAddress: Solidity.Address,
        transaction: Transaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Completable

    data class ExecuteInformation(
        val transactionHash: String,
        val transaction: Transaction,
        val sender: Solidity.Address,
        val requiredConfirmation: Int,
        val owners: List<Solidity.Address>
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
}
