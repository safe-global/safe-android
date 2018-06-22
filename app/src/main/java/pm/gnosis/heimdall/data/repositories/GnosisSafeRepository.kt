package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


interface GnosisSafeRepository {
    fun observeSafes(): Flowable<List<AbstractSafe>>
    fun observeDeployedSafes(): Flowable<List<Safe>>

    fun addSafe(address: Solidity.Address, name: String): Completable
    fun removeSafe(address: Solidity.Address): Completable
    fun updateSafe(safe: Safe): Completable
    fun loadSafe(address: Solidity.Address): Single<Safe>
    fun observeSafe(address: Solidity.Address): Flowable<Safe>

    fun loadInfo(address: Solidity.Address): Observable<SafeInfo>

    fun observePendingTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>
    fun observeSubmittedTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>

    fun savePendingSafe(transactionHash: BigInteger, name: String?, safeAddress: Solidity.Address, payment: Wei): Completable
    fun loadPendingSafe(transactionHash: BigInteger): Single<PendingSafe>
    fun removePendingSafe(transactionHash: BigInteger): Completable
    fun observePendingSafe(transactionHash: BigInteger): Flowable<PendingSafe>
    fun updatePendingSafe(pendingSafe: PendingSafe): Completable

    fun pendingSafeToDeployedSafe(pendingSafe: PendingSafe): Completable

    fun sendSafeCreationPush(safeAddress: Solidity.Address): Completable
}
