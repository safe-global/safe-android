package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import java.math.BigInteger


interface GnosisSafeRepository {
    fun observeSafes(): Flowable<List<Safe>>
    fun observeSafe(address: BigInteger): Flowable<Safe>
    fun add(address: BigInteger, name: String?): Completable
    fun remove(address: BigInteger): Completable
    fun updateName(address: BigInteger, newName: String): Completable
    fun loadInfo(address: BigInteger): Observable<SafeInfo>
    fun loadDescriptionCount(address: BigInteger): Observable<Int>
    fun loadDescriptions(address: BigInteger, from: Int, to: Int): Observable<List<String>>
}
