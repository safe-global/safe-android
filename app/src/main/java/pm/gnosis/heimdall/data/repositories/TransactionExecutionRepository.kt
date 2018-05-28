package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger


interface TransactionExecutionRepository {
    fun calculateHash(
        safeAddress: Solidity.Address, transaction: SafeTransaction,
        txGas: BigInteger, dataGas: BigInteger, gasPrice: BigInteger, gasToken: Solidity.Address = Solidity.Address(BigInteger.ZERO)
    ): Single<ByteArray>

    fun loadExecuteInformation(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<ExecuteInformation>
    fun sign(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Signature>

    fun checkSignature(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signature: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Pair<Solidity.Address, Signature>>

    fun observePublishStatus(id: String): Observable<PublishStatus>

    fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Completable

    data class ExecuteInformation(
        val transactionHash: String,
        val transaction: SafeTransaction,
        val sender: Solidity.Address,
        val requiredConfirmation: Int,
        val owners: List<Solidity.Address>,
        val gasPrice: BigInteger,
        val txGas: BigInteger,
        val dataGas: BigInteger,
        val balance: Wei
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

    enum class Operation {
        CALL,
        DELEGATE_CALL
    }
}
