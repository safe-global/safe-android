package pm.gnosis.heimdall.helpers

import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigInteger
import javax.inject.Inject


interface AddressStore {
    fun observe(): Observable<Set<BigInteger>>
    fun load(): Single<Set<BigInteger>>
    fun contains(entry: BigInteger): Boolean
    fun add(entry: BigInteger)
    fun remove(entry: BigInteger)
    fun clear()
}

class SimpleAddressStore @Inject constructor() : SetStore<BigInteger>(), AddressStore