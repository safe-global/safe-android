package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.remote.models.push.PushServiceTemporaryAuthorization
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

interface PushServiceRepository {
    fun syncAuthentication(forced: Boolean = false)
    fun pair(
        temporaryAuthorization: PushServiceTemporaryAuthorization,
        signingSafe: Solidity.Address?
    ): Single<Pair<AccountsRepository.SafeOwner, Solidity.Address>>

    fun propagateSafeCreation(safeAddress: Solidity.Address, targets: Set<Solidity.Address>): Completable
    fun propagateSubmittedTransaction(hash: String, chainHash: String, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable
    fun propagateTransactionRejected(hash: String, signature: Signature, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable
    fun observe(hash: String): Observable<TransactionResponse>
    fun requestConfirmations(
        hash: String,
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable

    fun handlePushMessage(pushMessage: PushMessage)
    fun calculateRejectionHash(transactionHash: ByteArray): Single<ByteArray>
    fun sendTypedDataConfirmation(hash: ByteArray, signature: ByteArray, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable

    sealed class TransactionResponse {
        data class Confirmed(val signature: Signature) : TransactionResponse()
        data class Rejected(val signature: Signature) : TransactionResponse()
    }

    fun requestTypedDataConfirmations(payload: String, appSignature: Signature, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable
    fun requestTypedDataRejection(hash: ByteArray, appSignature: Signature, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable

    fun observeTypedDataPushes(): Observable<PushMessage>
}
