package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.db.daos.DescriptionsDao
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger


interface GnosisSafeRepository {
    fun observeSafes(): Flowable<List<AbstractSafe>>
    fun observeSafe(address: Solidity.Address): Flowable<Safe>
    fun observeDeployedSafes(): Flowable<List<Safe>>

    fun addSafe(address: Solidity.Address, name: String): Completable
    fun removeSafe(address: Solidity.Address): Completable
    fun updateName(address: Solidity.Address, newName: String): Completable

    fun deploy(name: String, devices: Set<Solidity.Address>, requiredConfirmations: Int): Single<String>
    fun observeDeployStatus(hash: String): Observable<String>

    fun loadInfo(address: Solidity.Address): Observable<SafeInfo>
    fun observePendingTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>
    fun observeSubmittedTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>>
    fun loadSafeDeployTransaction(devices: Set<Solidity.Address>, requiredConfirmations: Int): Single<Transaction>
    fun savePendingSafe(transactionHash: BigInteger, name: String): Completable
    fun loadPendingSafe(transactionHash: BigInteger): Single<PendingSafe>
    fun loadSafe(address: Solidity.Address): Single<Safe>
}
