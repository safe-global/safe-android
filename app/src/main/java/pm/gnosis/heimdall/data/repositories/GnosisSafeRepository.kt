package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger


interface GnosisSafeRepository {
    fun observeAllSafes(): Flowable<List<AbstractSafe>>

    // Status
    fun loadInfo(address: Solidity.Address): Observable<SafeInfo>

    /**
     * Checks that at the passed address a valid safe exists. This is based on the proxy code and the master copy address
     */
    fun checkSafe(address: Solidity.Address): Observable<Boolean>

    // Deployed Safes
    fun observeSafes(): Flowable<List<Safe>>
    fun observeSafe(address: Solidity.Address): Flowable<Safe>
    fun loadSafe(address: Solidity.Address): Single<Safe>
    fun addSafe(address: Solidity.Address, name: String? = null): Completable
    fun removeSafe(address: Solidity.Address): Completable
    fun updateSafe(safe: Safe): Completable

    // Pending Safes
    fun observePendingSafe(address: Solidity.Address): Flowable<PendingSafe>
    fun loadPendingSafe(address: Solidity.Address): Single<PendingSafe>
    fun addPendingSafe(address: Solidity.Address, transactionHash: BigInteger, name: String?, payment: Wei): Completable
    fun removePendingSafe(address: Solidity.Address): Completable
    fun updatePendingSafe(pendingSafe: PendingSafe): Completable

    // Recovering Safes
    fun observeRecoveringSafe(address: Solidity.Address): Flowable<RecoveringSafe>
    fun loadRecoveringSafe(address: Solidity.Address): Single<RecoveringSafe>
    fun addRecoveringSafe(
        safeAddress: Solidity.Address,
        transactionHash: BigInteger?,
        name: String?,
        executeInfo: TransactionExecutionRepository.ExecuteInformation,
        signatures: List<Signature>
    ): Completable

    fun removeRecoveringSafe(address: Solidity.Address): Completable
    fun updateRecoveringSafe(recoveringSafe: RecoveringSafe): Completable

    // Safe Creation/ Recovery
    fun pendingSafeToDeployedSafe(pendingSafe: PendingSafe): Completable
    fun recoveringSafeToDeployedSafe(recoveringSafe: RecoveringSafe): Completable
    fun sendSafeCreationPush(safeAddress: Solidity.Address): Completable

    // Transactions
    fun observePendingTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>
    fun observeSubmittedTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>
}
