package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.models.Wei
import java.math.BigInteger


interface GnosisSafeRepository {
    fun observeSafes(): Flowable<List<AbstractSafe>>
    fun observeSafe(address: BigInteger): Flowable<Safe>
    fun observeDeployedSafes(): Flowable<List<Safe>>

    fun add(address: BigInteger, name: String?): Completable
    fun remove(address: BigInteger): Completable
    fun updateName(address: BigInteger, newName: String): Completable

    fun estimateDeployCosts(devices: Set<BigInteger>, requiredConfirmations: Int): Single<GasEstimate>
    fun deploy(name: String?, devices: Set<BigInteger>, requiredConfirmations: Int, overrideGasPrice: Wei?): Completable
    fun observeDeployStatus(hash: String): Observable<String>

    fun loadInfo(address: BigInteger): Observable<SafeInfo>
    fun observeTransactionDescriptions(address: BigInteger): Flowable<List<String>>
}
