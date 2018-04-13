package pm.gnosis.heimdall.helpers

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.model.Solidity
import javax.inject.Inject


interface AddressStore {
    fun observe(): Observable<Set<Solidity.Address>>
    fun load(): Single<Set<Solidity.Address>>
    fun contains(entry: Solidity.Address): Boolean
    fun add(entry: Solidity.Address)
    fun remove(entry: Solidity.Address)
    fun clear()
}

class SimpleAddressStore @Inject constructor() : SetStore<Solidity.Address>(), AddressStore
